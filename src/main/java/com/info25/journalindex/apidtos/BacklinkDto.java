package com.info25.journalindex.apidtos;

import lombok.Data;

@Data
public class BacklinkDto {
    private int id;
    private int from;
    private int to;
    private FileModifyDto toFile;
}
