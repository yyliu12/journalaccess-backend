package com.info25.journalindex.repositories;

import com.info25.journalindex.models.EventFile;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Collection of functions for interacting with event and file relationships
 */
public interface EventFileRepository extends CrudRepository<EventFile, Integer>, EventFileRepositoryCustom {
    EventFile findById(int id);
    void deleteById(int id);
    List<EventFile> findByEvent(int eventId);
    List<EventFile> findByFile(int fileId);
    void deleteByEvent(int eventId);
    void deleteByFile(int fileId);
}
