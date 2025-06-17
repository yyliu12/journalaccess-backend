package com.info25.journalindex.apidtos;

import com.info25.journalindex.models.Tag;

import lombok.Data;

@Data
public class TagJsTreeDto {
    int id;
    String parent;
    String text;
    String type;
    boolean children;
}
