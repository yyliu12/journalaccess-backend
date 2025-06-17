package com.info25.journalindex.repositories;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.info25.journalindex.apidtos.FileSearchDto;
import com.info25.journalindex.models.Backlink;
import com.info25.journalindex.models.File;

class CustomBacklinkRepositoryImpl implements CustomBacklinkRepository {
    @Autowired
    @Lazy
    BacklinkRepository backlinkRepository;

    @Autowired
    FileRepository fileRepository;

    @Override
    public void populateBacklinks(FileSearchDto f) {
        // Find all backlinks going to this file
        List<Backlink> backlinks = backlinkRepository.findByTo(f.getId());
        for (Backlink b : backlinks) {
            File fromFile = fileRepository.getWithoutSolr(b.getFrom());
            f.getBacklinks().add(FileSearchDto.fromFile(fromFile, null));
        }
    }
}