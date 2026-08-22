package com.info25.journalindex.models;

import com.info25.generated.tables.pojos.Tags;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

/**
 * Represents a tag
 */
@Data
@Table("tags")
public class Tag {
    @Id
    int id;
    String name;
    String fullName;
    int parent;
    @Column("is_folder")
    boolean container;

    public static Tag fromJooqTag(Tags jooqTag) {
        Tag tag = new Tag();
        tag.setId(jooqTag.getId().intValue());
        tag.setName(jooqTag.getName());
        tag.setFullName(jooqTag.getFullName());
        tag.setParent(jooqTag.getParent().intValue());
        tag.setContainer(jooqTag.getIsFolder());
        return tag;
    }
}
