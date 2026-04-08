package com.info25.journalindex.repositories;

import com.info25.journalindex.models.OOFile;
import org.springframework.data.repository.CrudRepository;

public interface OOFileRepository extends CrudRepository<OOFile, Integer> {
    OOFile findById(int id);

    void deleteById(int id);
}
