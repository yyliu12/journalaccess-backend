package com.info25.journalindex.repositories;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.info25.journalindex.apidtos.BacklinkDto;
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
            System.out.println(b.getFrom());
            File fromFile = fileRepository.getWithoutSolr(b.getFrom());
            f.getBacklinks().add(
                BacklinkDto.builder()
                    .id(b.getId())
                    .from(b.getFrom())
                    .to(b.getTo())
                    .annotation(b.getAnnotation())
                    .toFile(fileSearchDtoMapper.fileToFileModifyDto(fromFile))
                    .build()
            );
        }
    }
}