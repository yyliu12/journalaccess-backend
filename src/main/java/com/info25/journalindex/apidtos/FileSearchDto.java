package com.info25.journalindex.apidtos;

import com.info25.journalindex.models.File;
import com.info25.journalindex.models.Tag;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * File data returned to the client used for searching
 */
@Data
public class FileSearchDto {
    int id;
    String path;
    String highlight;
    List<File.Location> locations;
    List<Tag> tags;
    LocalDate date;
    String title;
    String description;
    // backlinks GOING to this file
    List<BacklinkDto> backlinks = new ArrayList<>();
    List<EventDto> events = new ArrayList<>();
    int parent;
    String attachmentCode;
    int journalId;

    public FileSearchDto setBacklinks(List<BacklinkDto> backlinks) {
        this.backlinks = backlinks;
        return this;
    }

    public FileSearchDto addBacklink(BacklinkDto backlink) {
        this.backlinks.add(backlink);
        return this;
    }
}
