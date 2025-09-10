package com.info25.journalindex.repositories;

import java.util.List;

import com.info25.journalindex.models.Tag;

/**
 * Collection of files responsible for interacting with the tag db
 */
public interface CustomTagRepository {
    List<Tag> findByManyIds(List<Integer> ids);
    List<Tag> findByName(String name);
    boolean hasChildren(int id);
    List<Tag> findRecursively(int id, boolean includeFolders);
}
