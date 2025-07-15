package com.info25.journalindex.apidtos;

import java.util.List;

import lombok.Data;

@Data
public class TagSearchOptions {
    List<Integer> tags;
    boolean includeFolders;
    boolean recursivelySearch;
    String combiningTerm; // AND or OR
}
