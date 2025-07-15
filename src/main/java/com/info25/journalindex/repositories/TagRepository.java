package com.info25.journalindex.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.info25.journalindex.models.Tag;

@Repository
public interface TagRepository extends CrudRepository<Tag, Integer>,  CustomTagRepository {
    Tag findById(int id);
    void deleteById(int id);
    List<Tag> findByFolder(int folder);
}