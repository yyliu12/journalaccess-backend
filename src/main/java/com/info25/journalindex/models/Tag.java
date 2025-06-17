package com.info25.journalindex.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

@Data
@Table("tags")
public class Tag {
    @Id
    int id;
    String name;
    String fullName;
    int folder;
    // 0 = NOT a folder
    // 1 = a folder
    int container;

    boolean isContainer() {
        return container == 1;
    }
}
