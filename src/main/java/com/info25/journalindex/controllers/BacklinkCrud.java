package com.info25.journalindex.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.info25.journalindex.apidtos.BacklinkDto;
import com.info25.journalindex.apidtos.FileModifyDto;
import com.info25.journalindex.apidtos.FileModifyDtoMapper;
import com.info25.journalindex.models.Backlink;
import com.info25.journalindex.repositories.BacklinkRepository;
import com.info25.journalindex.repositories.FileRepository;

@RestController
public class BacklinkCrud {
    @Autowired
    BacklinkRepository backlinkRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    FileModifyDtoMapper fileModifyDtoMapper;

    @PostMapping("/api/backlinks/get")
    public List<BacklinkDto> getBacklinks(@RequestParam("id") int id) {
        return backlinkRepository.findByFrom(id).stream().map(backlink -> {
            BacklinkDto dto = new BacklinkDto();
            dto.setId(backlink.getId());
            dto.setFrom(backlink.getFrom());
            dto.setTo(backlink.getTo());
            dto.setToFile(fileModifyDtoMapper.fileToFileModifyDto(fileRepository.getWithoutSolr(backlink.getTo())));
            return dto;
        }).collect(Collectors.toList());
    }

    @PostMapping("/api/backlinks/create")
    public Backlink createBacklink(@RequestParam("from") int from, @RequestParam("to") int to) {
        Backlink backlink = new Backlink();
        backlink.setFrom(from);
        backlink.setTo(to);
        return backlinkRepository.save(backlink);
    }

    @PostMapping("/api/backlinks/delete")
    public void deleteBacklink(@RequestParam("id") int id) {
        backlinkRepository.deleteById(id);
    }
}
