package com.info25.journalindex.models;

import com.info25.generated.tables.pojos.Events;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.persistence.Column;

/**
 * Represents an event -- a collection of files that have a common trait
 * e.g. being written on the same topic
 */
@Data
@Table("events")
public class Event {
    @Id
    int id;
    String name;
    // -1 means no parent
    int parent = -1;
    @Column(name = "is_folder")
    boolean isFolder;
    String description;

    public static Event fromJooqEvent(Events jooqEvent) {
        Event event = new Event();
        event.setId(jooqEvent.getId().intValue());
        event.setName(jooqEvent.getName());
        event.setParent(jooqEvent.getParent().intValue());
        event.setFolder(jooqEvent.getIsFolder());
        event.setDescription(jooqEvent.getDescription());
        return event;
    }
}
