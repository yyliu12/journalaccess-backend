package com.info25.journalindex.models;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.Id;


@Data
@Table("journals")
public class Journal {
	@Id
    int id;
    String name;
    String description;
    int color = 1;
}
