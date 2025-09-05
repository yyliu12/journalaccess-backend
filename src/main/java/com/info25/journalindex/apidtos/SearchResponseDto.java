package com.info25.journalindex.apidtos;

import lombok.Data;

import java.util.List;

/**
 * object to help send solr errors and the number of files found
 */
@Data
public class SearchResponseDto {
    int numFound;
    List<FileSearchDto> files;
    String error;
}
