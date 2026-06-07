package com.info25.journalindex.controllers;

import com.info25.journalindex.apidtos.EventFileDto;
import com.info25.journalindex.apidtos.EventDto;
import com.info25.journalindex.apidtos.FileSearchDtoMapper;
import com.info25.journalindex.models.EventFile;
import com.info25.journalindex.models.Event;
import com.info25.journalindex.repositories.EventFileRepository;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD functions for modifying which files belong to an event
 */
@RestController
@RequestMapping("/api/eventfile")
public class EventFileCrud {
    @Autowired
    EventFileRepository eventFileRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    FileSearchDtoMapper fileSearchDtoMapper;

    @Autowired
    EventRepository eventRepository;

    /**
     * Retrieves files asssociated with an event
     * @param fileId the id of the event to get files associated with
     * @return a list of EventFileDto
     */
    @PostMapping("/get")
    public List<EventFileDto> getEventFiles(@RequestParam("id") int fileId, @RequestParam("journals") int[] journals) {
        if (journals.length == 0) {
            journals = null;
        }

        List<EventFile> files = eventFileRepository.findByEvent(fileId, journals);

        return convertToDto(files, false);
    }

    @PostMapping("/getByFile")
    public List<EventFileDto> getFileEvents(@RequestParam("id") int fileId) {
        List<EventFile> files = eventFileRepository.findByFile(fileId);

        return convertToDto(files, true);
    }

    private List<EventFileDto> convertToDto(List<EventFile> files, boolean includeEvent) {
        List<EventFileDto> out = new ArrayList<>();

        for (EventFile ef : files) {
            EventFileDto dto = new EventFileDto();
            dto.setId(ef.getId());
            dto.setEventId(ef.getEvent());
            dto.setFileId(ef.getFile());
            dto.setFile(fileSearchDtoMapper.toDto(fileRepository.getById(ef.getFile())));
            if (includeEvent) {
                Event e = eventRepository.findById(ef.getEvent());
                dto.setEvent(EventDto.builder()
                            .name(e.getName())
                            .id(e.getId())
                            .parent(e.getParent())
                            .description(e.getDescription())
                            .build());
            }
            out.add(dto);
        }

        return out;
    }

    /**
     * Adds a file to an event
     * @param eventId the event id to add the file to
     * @param fileId the id of the file to add to the event
     * @return the id of the association
     */
    @PostMapping("/add")
    public int addEventFile(@RequestParam("event") int eventId, @RequestParam("file") int fileId) {
        EventFile eventFile = new EventFile();
        eventFile.setEvent(eventId);
        eventFile.setFile(fileId);
        eventFileRepository.saveSafe(eventFile);
        return eventFile.getId();
    }

    /**
     * Deletes a file from an event
     * @param id the id of the eventfile association
     * @return OK
     */
    @PostMapping("/remove")
    public String removeEventFile(@RequestParam("id") int id) {
        EventFile eventFile = eventFileRepository.findById(id);
        if (eventFile != null) {
            eventFileRepository.deleteSafe(eventFile);
        }

        return "OK";
    }

}
