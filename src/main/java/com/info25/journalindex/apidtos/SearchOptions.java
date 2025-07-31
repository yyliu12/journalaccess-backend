package com.info25.journalindex.apidtos;

import java.util.List;

import lombok.Data;

@Data
public class SearchOptions {
    TagSearchOptions tags;
    List<Integer> events;
    String sort;
    String query;
}
