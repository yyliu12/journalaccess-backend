package com.info25.journalindex.apidtos;

import lombok.Data;

/**
 * DTO to give to jsTree
 */
@Data
public class TagJsTreeDto {
    int id;
    String parent;
    String text;
    String type;
    boolean children;
}
