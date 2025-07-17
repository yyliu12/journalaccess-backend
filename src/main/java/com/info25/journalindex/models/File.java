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
    /**
     * === Stored in both SQL and Solr ===
     */
    int id = -1;
    String path;
    LocalDate date;
    String uuid;
    /**
     * === Stored in Solr only ===
     */
    String content;
    ArrayList<Location> locations;
    // An arraylist of tag IDs which can be looked up in the SQL db.
    ArrayList<Integer> tags;
    /**
     * XML or JSON annotation data depending on the file type.
     * 
     * pdf = XML xfdf annotation data
     * image (png, jpg, jpeg) = annotorious seadragon JSON data
     * html = annotator.js JSON data
     */
    String annotations;
    /**
     * === Stored in SQL only ===
     */
    // Raw text of annotation content for searching in Solr.
    String annotationContent;
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

        public Location(String coordinate, String address) {
            this.coordinate = coordinate;
            this.address = address;
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

    public String getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String annotations) {
        this.annotations = annotations;
    }

    public String getAnnotationContent() {
        return annotationContent;
    }

    public void setAnnotationContent(String annotationContent) {
        this.annotationContent = annotationContent;
    }
}
