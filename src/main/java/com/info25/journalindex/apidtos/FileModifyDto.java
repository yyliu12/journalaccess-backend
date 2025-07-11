package com.info25.journalindex.apidtos;

import java.time.LocalDate;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.info25.journalindex.models.File.Location;

import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileModifyDto {
    int id = -1;
    String path;
    LocalDate date;
    String uuid;
    String content;
    ArrayList<Location> locations;
    ArrayList<Integer> tags;

    public FileModifyDto() {}
}
