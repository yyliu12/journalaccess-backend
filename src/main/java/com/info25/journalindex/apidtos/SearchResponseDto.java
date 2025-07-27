package com.info25.journalindex.apidtos;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponseDto {
    int numFound;
    List<FileSearchDto> files;
    String error;
}
