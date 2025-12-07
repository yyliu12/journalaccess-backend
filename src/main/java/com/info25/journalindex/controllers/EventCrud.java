package com.info25.journalindex.controllers;

import com.info25.journalindex.apidtos.EventDto;
import com.info25.journalindex.models.Event;
import com.info25.journalindex.repositories.EventFileRepository;
import com.info25.journalindex.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD functions for events, a way of grouping files together
 */
@RestController
@RequestMapping("/api/event")
public class EventCrud {
    @Autowired
    EventRepository eventRepository;

    @Autowired
    EventFileRepository eventFileRepository;

    /**
     * Gets a list of events with the parent id specified
     * @param id the parent id of the events; -1 is the root
     * @return a list of events
     */
    @PostMapping("/get")
    List<EventDto> getEvents(@RequestParam("id") int id) {
        List<Event> events = eventRepository.findByParent(id);
        List<EventDto> output = new ArrayList<>();

        for (Event e : events) {
            output.add(
                    EventDto.builder()
                            .name(e.getName())
                            .id(e.getId())
                            .parent(e.getParent())
                            .description(e.getDescription())
                            .isFolder(e.isFolder())
                            .hasChildren(eventRepository.existsByParent(e.getId()))
                            .build());
        }

        return output;
    }

    /**
     * Creates an event based on the specified properties
     * @param name the name of the event
     * @param parent the id of the event's parent
     * @param description a brief description of the event
     * @param isFolder whether the event holds other events or not
     * @return the event id
     */
    @PostMapping("/create")
    int createEvent(
            @RequestParam("name") String name,
            @RequestParam("parent") int parent,
            @RequestParam("description") String description,
            @RequestParam("isFolder") boolean isFolder) {
        Event event = new Event();
        event.setName(name);
        event.setParent(parent);
        event.setDescription(description);
        event.setFolder(isFolder);
        eventRepository.save(event);
        return event.getId();
    }

    /**
     * Modifies an event based on the given properties
     * @param id the id of the event to modify
     * @param name the name of the event
     * @param parent what the event's parent is
     * @param description the description of the event
     * @return the id of the event
     */
    @PostMapping("/modify")
    int modifyEvent(
            @RequestParam("id") int id,
            @RequestParam("name") String name,
            @RequestParam("parent") int parent,
            @RequestParam("description") String description) {
        Event event = eventRepository.findById(id);
        if (event == null) {
            return -1; // Event not found
        }
        event.setName(name);
        event.setParent(parent);
        event.setDescription(description);
        eventRepository.save(event);
        return id;
    }

    /**
     * Deletes a specified event
     * @param id the id of the event to delete
     * @return OK
     */
    @PostMapping("/delete")
    String deleteEvent(@RequestParam("id") int id) {
        if (!eventRepository.existsById(id)) {
            return "Event not found";
        }
        Event event = eventRepository.findById(id);

        eventRepository.moveChildrenToNewParent(id, event.getParent());

        eventRepository.deleteById(id);
        eventFileRepository.deleteByEventSafe(id);

        return "OK";
    }
}
