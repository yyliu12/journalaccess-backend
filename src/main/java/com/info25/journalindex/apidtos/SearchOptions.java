package com.info25.journalindex.apidtos;

import lombok.Data;

@Data
public class SearchOptions {
    TagSearchOptions tags;
    String sort;
    String query;
}
