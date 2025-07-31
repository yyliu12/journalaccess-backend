package com.info25.journalindex.apidtos;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * Java object representing search JSON created by SearchCreator
 * on the frontend.
 */
@Data
public class SearchOptions {
    TagSearchOptions tags;
    List<Integer> events;
    String sort;
    String query;
    // these are inclusive
    boolean dateFilteringEnabled;
    LocalDate startDate;
    LocalDate endDate;
}
