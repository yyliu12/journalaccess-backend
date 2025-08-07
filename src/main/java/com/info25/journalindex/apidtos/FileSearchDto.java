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
    String title;
    String description;
    // backlinks GOING to this file
    List<BacklinkDto> backlinks = new ArrayList<>();
    List<EventDto> events = new ArrayList<>();
    int parent;
    String attachmentCode;

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

    public List<BacklinkDto> getBacklinks() {
        return backlinks;
    }

    public FileSearchDto setBacklinks(List<BacklinkDto> backlinks) {
        this.backlinks = backlinks;
        return this;
    }

    public FileSearchDto addBacklink(BacklinkDto backlink) {
        this.backlinks.add(backlink);
        return this;
    }

    public String getTitle() {
        return title;
    }

    public FileSearchDto setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public FileSearchDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<EventDto> getEvents() {
        return events;
    }

    public FileSearchDto setEvents(List<EventDto> events) {
        this.events = events;
        return this;
    }

    public int getParent() {
        return parent;
    }

    public FileSearchDto setParent(int parent) {
        this.parent = parent;
        return this;
    }

    public String getAttachmentCode() {
        return attachmentCode;
    }

    public FileSearchDto setAttachmentCode(String attachmentCode) {
        this.attachmentCode = attachmentCode;
        return this;
    }

}
