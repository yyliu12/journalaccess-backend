package com.info25.journalindex.repositories;

import com.info25.journalindex.apidtos.FileSearchDto;

import java.util.List;

public interface CustomBacklinkRepository {
    void populateBacklinks(FileSearchDto f);
    void populateManyBacklinks(List<FileSearchDto> files);
}
