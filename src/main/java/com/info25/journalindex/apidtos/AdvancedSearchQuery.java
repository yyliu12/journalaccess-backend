package com.info25.journalindex.apidtos;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
public class AdvancedSearchQuery {
    String sort;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NestedAdvancedSearchQuery {
        // 0 = text filter
        // 1 = date filter
        // 2 = tag filter
        // 3 = event filter
        // 4 = journal filter
        // 5 = combo filter
        int type;
        String query;
        DateSearchOptions dateOptions;
        TagSearchOptions tags;
        int[] events;
        int[] journals;
        String combiningTerm; // AND or OR
        NestedAdvancedSearchQuery[] nestedQueries;
    }

    NestedAdvancedSearchQuery query;
}
