package com.info25.journalindex.repositories;

import com.info25.journalindex.models.EventFile;

public interface EventFileRepositoryCustom {
    void deleteByEventSafe(int eventId);
    void deleteByFileSafe(int fileId);
    void deleteSafe(EventFile ef);
}
