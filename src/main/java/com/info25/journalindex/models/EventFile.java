package com.info25.journalindex.models;

import com.info25.generated.tables.pojos.EventsFile;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Represents a file that is part of an event.
 */
@Data
@Table("events_file")
public class EventFile {
    @Id
    int id;
    @Column("FILE_ID")
    int file;
    int event;

    public static EventFile fromJooqEventFile(EventsFile jooqEventFile) {
        EventFile eventFile = new EventFile();
        eventFile.setId(jooqEventFile.getId().intValue());
        eventFile.setFile(jooqEventFile.getFileId().intValue());
        eventFile.setEvent(jooqEventFile.getEvent().intValue());
        return eventFile;
    }
}
