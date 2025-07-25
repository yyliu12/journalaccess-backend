package com.info25.journalindex.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

/**
 * Represents a backlink made between two files. A backlink goes
 * in one direction. It has a origin and a target. The backlink
 * badge in the UI only displays on the target.
 */
@Data
@Table("backlinks")
public class Backlink {
    @Id
    int id;
    @Column("origin")
    int from;
    @Column("target")
    int to;
    @Column("annotation")
    String annotation;
}
