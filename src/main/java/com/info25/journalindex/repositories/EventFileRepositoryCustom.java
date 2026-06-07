package com.info25.journalindex.repositories;

import java.util.List;

import com.info25.journalindex.models.EventFile;

public interface EventFileRepositoryCustom {
    void deleteByEventSafe(int eventId);
    void deleteSafe(EventFile ef);
    void saveSafe(EventFile ef);
    List<EventFile> findByEvent(int eventId, int[] journals);
}
