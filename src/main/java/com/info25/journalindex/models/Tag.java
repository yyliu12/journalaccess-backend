package com.info25.journalindex.models;

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
}
