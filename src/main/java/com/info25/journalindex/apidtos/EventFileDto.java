package com.info25.journalindex.apidtos;

import lombok.Data;

@Data
public class EventFileDto {
    int id;
    int fileId;
    int eventId;
    FileSearchDto file;
}
