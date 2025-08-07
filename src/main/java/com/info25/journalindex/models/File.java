package com.info25.journalindex.models;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * A file represents a file uploaded to the system as part of a journal entry.
 * A file belongs to a specific day, and may have tags, locations, and backlinks
 * associated with it. Solr stores the raw text content of the file (for searching 
 * purposes), while the actual file is stored on the filesystem.
 */
public class File {
    int id = -1;
    String path;
    LocalDate date;
    String uuid;
    String content;
    ArrayList<Location> locations;
    ArrayList<Integer> tags;
    String annotation;
    String title;
    String description;
    int parent = -1; // -1 means no parent
    /** 
     * These variables are used to save date and path modifications to the filesystem
     * since we need to know the original date & path in order to rename a file.
     * 
     * These should not be modified directly.
     * 
     * These are not stored in Solr or SQL
     */
    boolean __dateModified = false;
    boolean __pathModified = false;
    LocalDate __originalDate = null;
    String __originalPath = null;

    static public class Location {
        String coordinate;
        String address;
        String buildingName;

        public Location(String coordinate, String address, String buildingName) {
            this.coordinate = coordinate;
            this.address = address;
            this.buildingName = buildingName;
        }

        public Location() {}

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

        public String getBuildingName() {
            return buildingName;
        }

        public void setBuildingName(String buildingName) {
            this.buildingName = buildingName;
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
        // Track the original path
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
        // Track the original date
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

    // Called by repository after saving the file since the file now exists
    // at the location described by date & path. Should only be called by
    // FileRepository.
    public void __savedByRepository() {
        this.__dateModified = false;
        this.__pathModified = false;
        this.__originalDate = null;
        this.__originalPath = null;
    }

    public boolean __isDateModified() {
        return this.__dateModified;
    }

    public boolean __isPathModified() {
        return this.__pathModified;
    }

    public String __getOriginalPath() {
        return this.__originalPath;
    }

    public LocalDate __getOriginalDate() {
        return this.__originalDate;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }
}
