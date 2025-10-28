package com.info25.journalindex.repositories;


import com.info25.journalindex.models.Journal;
import org.springframework.data.repository.CrudRepository;

public interface JournalRepository extends CrudRepository<Journal, Integer> {
    Journal findById(int id);
    void deleteById(int id);
}
