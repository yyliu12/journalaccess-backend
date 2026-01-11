package com.info25.journalindex.repositories;

import com.info25.journalindex.models.EventFile;
import com.info25.journalindex.util.SolrUpdateBuffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    @Override
    public void deleteByEventSafe(int eventId) {
        String sql = "DELETE FROM events_file WHERE event = ?";
        List<EventFile> filesAffected = eventFileRepository.findByEvent(eventId);

        jdbcTemplate.update(sql, eventId);

        SolrUpdateBuffer solrUpdateBuffer = new SolrUpdateBuffer();
        for (EventFile eventFile : filesAffected) {
            fileRepository.saveToSolrBuffer(eventFile.getFile(), solrUpdateBuffer);
        }
        fileRepository.saveSolrBuffer(solrUpdateBuffer);
    }

    @Override
    public void deleteByFileSafe(int fileId) {
        String sql = "DELETE FROM events_file WHERE file = ?";

        jdbcTemplate.update(sql, fileId);

        fileRepository.save(fileRepository.getById(fileId));
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
}
