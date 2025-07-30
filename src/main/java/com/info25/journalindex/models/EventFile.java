package com.info25.journalindex.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Represents a file that is part of an event.
 */
@Data
@Table("events_file")
public class EventFile {
    @Id
    int id;
    int file;
    int event;
}
