package com.info25.journalindex.models;
import java.time.LocalDate;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.info25.journalindex.apidtos.FileSearchDto;


public class File {
    int id = -1;
    String path;
    LocalDate date;
    String uuid;
    String content;
    ArrayList<Location> locations;
    ArrayList<Integer> tags;
    boolean __dateModified = false;
    boolean __pathModified = false;
    LocalDate __originalDate = null;
    String __originalPath = null;

    static public class Location {
        String coordinate;
        String address;

        public Location(String coordinate, String address) {
            this.coordinate = coordinate;
            this.address = address;
        }

        public String getCoordinate() {
            return coordinate;
        }

        public void setCoordinate(String coordinate) {
            this.coordinate = coordinate;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

    }

    public File(int id, String path, LocalDate date, String uuid) {
        this.id = id;
        this.path = path;
        this.date = date;
        this.uuid = uuid;
        this.locations = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    

    public File () {
        this.locations = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (this.path != null && !this.path.equals(path) && __originalPath == null) {
            this.__pathModified = true;
            this.__originalPath = this.path;
        }
        this.path = path;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        if (this.date != null && !this.date.equals(date) && __originalDate == null) {
            this.__dateModified = true;
            this.__originalDate = this.date;
        }
        this.date = date;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public ArrayList<Location> getLocations() {
        return locations;
    }

    public void setLocations(ArrayList<Location> locations) {
        this.locations = locations;
    }

    public void addLocation(Location location) {
        this.locations.add(location);
    }

    public void removeLocation(int index) {
        this.locations.remove(index);
    }

    public ArrayList<Integer> getTags() {
        return tags;
    }

    public void setTags(ArrayList<Integer> tags) {
        this.tags = tags;
    }

    public void addTag(int tag) {
        this.tags.add(tag);
    }

    public void removeTag(int tag) {
        this.tags.remove(Integer.valueOf(tag));
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void __savedByRepository() {
        this.__dateModified = false;
        this.__pathModified = false;
        this.__originalDate = null;
        this.__originalPath = null;
    }
}
