package com.info25.journalindex.apidtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BacklinkDto {
    private int id;
    private int from;
    private int to;
    private String annotation;
    private FileModifyDto toFile;
    private boolean display;
}
