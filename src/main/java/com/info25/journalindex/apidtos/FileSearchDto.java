package com.info25.journalindex.apidtos;

import com.info25.journalindex.models.File;
import com.info25.journalindex.models.Tag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FileSearchDto {
    int id;
    String path;
    String highlight;
    List<File.Location> locations;
    List<Tag> tags;
    LocalDate date;
    // backlinks GOING to this file
    List<FileSearchDto> backlinks = new ArrayList<>();

    public LocalDate getDate() {
        return date;
    }

    public FileSearchDto setDate(LocalDate date) {
        this.date = date;
        return this;
    }
    public int getId() {
        return id;
    }

    public FileSearchDto setId(int id) {
        this.id = id;
        return this;
    }

    public String getPath() {
        return path;
    }

    public FileSearchDto setPath(String path) {
        this.path = path;
        return this;
    }

    public String getHighlight() {
        return highlight;
    }

    public FileSearchDto setHighlight(String highlight) {
        this.highlight = highlight;
        return this;
    }

    public List<File.Location> getLocations() {
        return locations;
    }

    public FileSearchDto setLocations(List<File.Location> locations) {
        this.locations = locations;
        return this;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public FileSearchDto setTags(List<Tag> tags) {
        this.tags = tags;
        return this;
    }

    public List<FileSearchDto> getBacklinks() {
        return backlinks;
    }

    public FileSearchDto setBacklinks(List<FileSearchDto> backlinks) {
        this.backlinks = backlinks;
        return this;
    }

    public FileSearchDto addBacklink(FileSearchDto backlink) {
        this.backlinks.add(backlink);
        return this;
    }

    public static FileSearchDto fromFile (File f, String highlight) {
        return new FileSearchDto()
                .setHighlight(highlight)
                .setId(f.getId())
                .setPath(f.getPath())
                .setLocations(f.getLocations())
                .setDate(f.getDate());
    }
}
