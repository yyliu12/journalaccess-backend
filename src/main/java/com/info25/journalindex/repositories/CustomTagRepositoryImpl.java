package com.info25.journalindex.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.info25.generated.tables.pojos.Tags;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.info25.journalindex.apidtos.FileSearchDto;
import com.info25.journalindex.models.Tag;

import static com.info25.generated.tables.Tags.TAGS;

public class CustomTagRepositoryImpl implements CustomTagRepository {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    TagRepository tagRepository;

    @Autowired
    @Lazy
    FileRepository fileRepository;

    @Autowired
    DSLContext dsl;

    /**
     * Returns a list of tag data by a list of ids
     */
    @Override
    public List<Tag> findByManyIds(List<Integer> ids) {
        return dsl.select(TAGS.asterisk())
                .from(TAGS)
                .where(TAGS.ID.in(ids))
                .fetchInto(Tags.class)
                .stream()
                .map(x -> Tag.fromJooqTag(x))
                .collect(Collectors.toList());
    }

    /**
     * Returns tag data when the name string appears in either the name or full_name field
     */
    @Override
    public List<Tag> findByName(String name) {
        return dsl.select(TAGS.asterisk())
                .from(TAGS)
                .where(TAGS.NAME.containsIgnoreCase(name)
                        .or(TAGS.FULL_NAME.containsIgnoreCase(name)))
                .fetchInto(Tags.class)
                .stream()
                .map(x -> Tag.fromJooqTag(x))
                .collect(Collectors.toList());
    }

    /**
     * True or false: whether the tag has children
     */
    @Override
    public boolean hasChildren(int id) {
        return dsl.fetchExists(dsl.selectOne()
                .from(TAGS)
                .where(TAGS.PARENT.eq(new BigDecimal(id))));
    }

    /**
     * Recursively finds tags under a given tag id. 
     */
    @Override
    public List<Tag> findRecursively(int id, boolean includeFolders) {
        List<Tag> tags = dsl.select(TAGS.asterisk())
                .from(TAGS)
                .startWith(TAGS.ID.eq(new BigDecimal(id)))
                .connectBy(DSL.prior(TAGS.ID).eq(TAGS.PARENT))
                .fetchInto(Tags.class)
                .stream()
                .map(x -> Tag.fromJooqTag(x))
                .collect(Collectors.toList());

        if (!includeFolders) {
            return tags.stream().filter(tag -> !tag.isContainer()).collect(Collectors.toList());
        }

        return tags;
    }
}
