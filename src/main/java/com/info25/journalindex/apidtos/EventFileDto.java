package com.info25.journalindex.apidtos;

import lombok.Data;

/**
 * Represents an event file association sent to the client
 */
@Data
public class EventFileDto {
    int id;
    int fileId;
    int eventId;
    // the data of the file
    FileSearchDto file;
}
