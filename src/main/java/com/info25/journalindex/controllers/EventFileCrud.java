package com.info25.journalindex.controllers;

import com.info25.journalindex.apidtos.EventFileDto;
import com.info25.journalindex.apidtos.FileSearchDto;
import com.info25.journalindex.apidtos.FileSearchDtoMapper;
import com.info25.journalindex.models.EventFile;
import com.info25.journalindex.repositories.EventFileRepository;
import com.info25.journalindex.repositories.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/eventfile")
public class EventFileCrud {
    @Autowired
    EventFileRepository eventFileRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    FileSearchDtoMapper fileSearchDtoMapper;

    @PostMapping("/get")
    public List<EventFileDto> getEventFiles(@RequestParam("id") int fileId) {

        List<EventFile> files = eventFileRepository.findByEvent(fileId);
        List<EventFileDto> out = new ArrayList<>();

        for (EventFile ef : files) {
            EventFileDto dto = new EventFileDto();
            dto.setId(ef.getId());
            dto.setEventId(ef.getEvent());
            dto.setFileId(ef.getFile());
            dto.setFile(fileSearchDtoMapper.toDto(fileRepository.getById(ef.getFile())));
            out.add(dto);
        }
        return out;
    }

    @PostMapping("/add")
    public int addEventFile(@RequestParam("event") int eventId, @RequestParam("file") int fileId) {
        EventFile eventFile = new EventFile();
        eventFile.setEvent(eventId);
        eventFile.setFile(fileId);
        eventFileRepository.save(eventFile);
        return eventFile.getId();
    }

    @PostMapping("/remove")
    public String removeEventFile(@RequestParam("id") int id) {
        EventFile eventFile = eventFileRepository.findById(id);
        if (eventFile != null) {
            eventFileRepository.delete(eventFile);
        }

        return "OK";
    }

}
