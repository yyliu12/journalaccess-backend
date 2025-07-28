package com.info25.journalindex.repositories;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.info25.journalindex.apidtos.BacklinkDto;
import com.info25.journalindex.apidtos.FileModifyDto;
import com.info25.journalindex.apidtos.FileModifyDtoMapper;
import com.info25.journalindex.apidtos.FileSearchDto;
import com.info25.journalindex.models.Backlink;
import com.info25.journalindex.models.File;

class CustomBacklinkRepositoryImpl implements CustomBacklinkRepository {
    @Autowired
    @Lazy
    BacklinkRepository backlinkRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    FileModifyDtoMapper fileSearchDtoMapper;

    @Override
    public void populateBacklinks(FileSearchDto f) {
        // Find all backlinks going to this file
        List<Backlink> backlinks = backlinkRepository.findByTo(f.getId());
        for (Backlink b : backlinks) {
            // don't give client non displayed backlinks -- these are only available
            // when queried on the viewfile screen
            if (!b.isDisplay()) 
                continue;
            
            File fromFile = fileRepository.getById(b.getFrom());
            FileModifyDto fileModifyDto = new FileModifyDto();

            // we only need a limited set of properties here
            fileModifyDto.setId(fromFile.getId());
            fileModifyDto.setPath(fromFile.getPath());
            fileModifyDto.setDate(fromFile.getDate());

            f.getBacklinks().add(
                BacklinkDto.builder()
                    .id(b.getId())
                    .from(b.getFrom())
                    .to(b.getTo())
                    .annotation(b.getAnnotation())
                    .toFile(fileModifyDto)
                    .build()
            );
        }
    }
}