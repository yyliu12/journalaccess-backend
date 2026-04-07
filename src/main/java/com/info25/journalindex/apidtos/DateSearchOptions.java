package com.info25.journalindex.apidtos;

import java.time.LocalDate;

import lombok.Data;

@Data
public class DateSearchOptions {
    boolean dateFilteringEnabled;
    LocalDate startDate;
    LocalDate endDate;
}
