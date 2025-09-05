package com.info25.journalindex.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.info25.journalindex.apidtos.BacklinkDto;
import com.info25.journalindex.apidtos.FileModifyDtoMapper;
import com.info25.journalindex.models.Backlink;
import com.info25.journalindex.repositories.BacklinkRepository;
import com.info25.journalindex.repositories.FileRepository;

/**
 * Rest controller responsible for CRUD for backlinks
 */
@RestController
public class BacklinkCrud {
    @Autowired
    BacklinkRepository backlinkRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    FileModifyDtoMapper fileModifyDtoMapper;
    /**
     * Requests backlink info on a file
     * @param id The id of the file
     * @return A list of backlinks **originating** from that file
     */
    @PostMapping("/api/backlinks/get")
    public List<BacklinkDto> getBacklinks(@RequestParam("id") int id) {
        return backlinkRepository.findByFrom(id).stream().map(backlink -> {
            BacklinkDto dto = new BacklinkDto();
            dto.setId(backlink.getId());
            dto.setFrom(backlink.getFrom());
            dto.setTo(backlink.getTo());
            dto.setAnnotation(backlink.getAnnotation());
            dto.setDisplay(backlink.isDisplay());
            dto.setToFile(fileModifyDtoMapper.fileToFileModifyDto(fileRepository.getById(backlink.getTo())));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Requests backlinks to a file
     * @param id The id of the file
     * @return A list of backlinks **going** to that file
     */
    @PostMapping("/api/backlinks/getByTo")
    public List<BacklinkDto> getBacklinksByTo(@RequestParam("id") int id) {
        return backlinkRepository.findByTo(id).stream().map(backlink -> {
            BacklinkDto dto = new BacklinkDto();
            dto.setId(backlink.getId());
            dto.setFrom(backlink.getFrom());
            dto.setTo(backlink.getTo());
            dto.setAnnotation(backlink.getAnnotation());
            dto.setDisplay(backlink.isDisplay());
            dto.setToFile(fileModifyDtoMapper.fileToFileModifyDto(fileRepository.getById(backlink.getFrom())));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Creates a backlink based on the specified properties
     * @param from what file id the backlink originates from
     * @param to what file id the backlink goes to
     * @param annotation the text attached to the backlink
     * @param display whether to display the backlink
     * @return the backlink id
     */
    @PostMapping("/api/backlinks/create")
    public int createBacklink(
        @RequestParam("from") int from, 
        @RequestParam("to") int to, 
        @RequestParam("annotation") String annotation,
        @RequestParam("display") boolean display
    ) {
        Backlink backlink = new Backlink();
        backlink.setFrom(from);
        backlink.setTo(to);
        backlink.setAnnotation(annotation);
        backlink.setDisplay(display);
        backlinkRepository.save(backlink);
        return backlink.getId();
    }

    /**
     * Updates a backlink
     * @param id the id of the backlink to update
     * @param to what file id the backlink goes to
     * @param annotation what annotation text is attached to the backlink
     * @param display whether to display the backlink
     * @return OK
     */
    @PostMapping("/api/backlinks/update")
    public String updateBacklink(
        @RequestParam("id") int id, 
        @RequestParam("to") int to, 
        @RequestParam("annotation") String annotation,
        @RequestParam("display") boolean display
    ) {
        Backlink backlink = backlinkRepository.findById(id);
        backlink.setTo(to);
        backlink.setAnnotation(annotation);
        backlink.setDisplay(display);
        backlinkRepository.save(backlink);
        // TODO: Index backlinks
        return "OK";
    }

    /**
     * Deletes a backlink
     * @param id the id of the backlink to delete
     */
    @PostMapping("/api/backlinks/delete")
    public void deleteBacklink(@RequestParam("id") int id) {
        backlinkRepository.deleteById(id);
    }
}
