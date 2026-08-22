package com.info25.journalindex.repositories;

import com.info25.generated.tables.pojos.EventsFile;
import com.info25.journalindex.models.EventFile;
import com.info25.journalindex.util.SolrUpdateBuffer;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.stereotype.Repository;

import static com.info25.generated.tables.EventsFile.EVENTS_FILE;
import static com.info25.generated.tables.Files.FILES;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class EventFileRepositoryCustomImpl implements EventFileRepositoryCustom {
    @Autowired
    @Lazy
    EventFileRepository eventFileRepository;

    @Autowired
    @Lazy
    FileRepository fileRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DSLContext dsl;

    @Override
    public void deleteByEventSafe(int eventId) {
        dsl.delete(EVENTS_FILE)
            .where(EVENTS_FILE.EVENT.eq(new BigDecimal(eventId)))
            .execute();
    }

    @Override
    public void deleteSafe(EventFile ef) {
        int fileId = ef.getFile();
        eventFileRepository.delete(ef);

        fileRepository.save(fileRepository.getById(fileId));
    }

    @Override
    public void saveSafe(EventFile ef) {
        eventFileRepository.save(ef);
        fileRepository.save(fileRepository.getById(ef.getFile()));
    }

    @Override
    public List<EventFile> findByEvent(int eventId, int[] journals) {
        return dsl.select(EVENTS_FILE.asterisk())
                .from(EVENTS_FILE)
                .leftJoin(FILES).on(EVENTS_FILE.FILE_ID.eq(FILES.ID))
                .where(EVENTS_FILE.EVENT.eq(new BigDecimal(eventId)))
                .and(journals == null ? DSL.trueCondition() : FILES.JOURNAL_ID.in(Arrays.asList(journals)))
                .fetchInto(EventsFile.class)
                .stream()
                .map(EventFile::fromJooqEventFile)
                .collect(Collectors.toList());
    }
}
