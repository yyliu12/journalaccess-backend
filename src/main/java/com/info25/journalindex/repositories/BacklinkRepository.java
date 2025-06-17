package com.info25.journalindex.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.info25.journalindex.models.Backlink;
    
public interface BacklinkRepository extends CrudRepository<Backlink, Integer>, CustomBacklinkRepository {
    Backlink findById(int id);
    void deleteById(int id);
    List<Backlink> findByFrom(int from);
    List<Backlink> findByTo(int to);
}
