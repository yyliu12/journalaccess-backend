package com.info25.journalindex.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

@Data
@Table("backlinks")
public class Backlink {
    @Id
    int id;
    @Column("origin")
    int from;
    @Column("target")
    int to;
}
