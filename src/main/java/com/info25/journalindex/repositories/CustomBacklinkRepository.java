package com.info25.journalindex.repositories;

import com.info25.journalindex.apidtos.FileSearchDto;

public interface CustomBacklinkRepository {
    void populateBacklinks(FileSearchDto f);
}
