package com.info25.journalindex.models;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    List<Integer> tags = new ArrayList<>();
    boolean tagsChanged = false;
    String annotation;
    String title;
    String description;
    int parent = -1; // -1 means no parent
    String attachmentCode;
    int journalId = 1;
    int ooFileId = -1;
    boolean isLegacyOnlineEditorFile = false;
    boolean isCKEditorFile = false;
    LocalDate writtenDate;
    boolean isAsciidoc = false;
    List<Integer> locationIds = new ArrayList<>();
    boolean locationIdsChanged = false;
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

    public List<Integer> getTags() {
        return tags;
    }

    public void setTags(List<Integer> tags) {
        this.tagsChanged = true;
        this.tags = tags;
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
        this.locationIdsChanged = false;
        this.tagsChanged = false;
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

    public String getAttachmentCode() {
        return attachmentCode;
    }

    public void setAttachmentCode(String attachmentCode) {
        this.attachmentCode = attachmentCode;
    }

    public int getJournalId() {
        return journalId;
    }

    public void setJournalId(int journalId) {
        this.journalId = journalId;
    }

    public int getOOFileId() {
        return ooFileId;
    }

    public void setOOFileId(int ooFileId) {
        this.ooFileId = ooFileId;
    }

    public boolean isLegacyOnlineEditorFile() {
        return isLegacyOnlineEditorFile;
    }

    public void setLegacyOnlineEditorFile(boolean legacyOnlineEditorFile) {
        isLegacyOnlineEditorFile = legacyOnlineEditorFile;
    }

    public boolean isCKEditorFile() {
        return isCKEditorFile;
    }

    public void setCKEditorFile(boolean CKEditorFile) {
        isCKEditorFile = CKEditorFile;
    }

    public LocalDate getWrittenDate() {
        return writtenDate;
    }

    public void setWrittenDate(LocalDate writtenDate) {
        this.writtenDate = writtenDate;
    }

    public boolean isAsciidoc() {
        return isAsciidoc;
    }

    public void setAsciidoc(boolean asciidoc) {
        isAsciidoc = asciidoc;
    }

    public List<Integer> getLocationIds() {
        return locationIds;
    }

    public void setLocationIds(List<Integer> locationIds) {
        this.locationIdsChanged = true;
        this.locationIds = locationIds;
    }

    // As part of the Jooq transition we need to convert this
    // to the Jooq generated version
    public JooqFile toJooqFile() {
        JooqFile jooqFile = new JooqFile();
        jooqFile.setId(new BigDecimal(this.id));
        jooqFile.setPath(this.path);
        jooqFile.setFileDate(this.date);
        jooqFile.setUuid(this.uuid);
        jooqFile.setContent(this.content);
        jooqFile.setAnnotation(this.annotation);
        jooqFile.setTitle(this.title);
        jooqFile.setDescription(this.description);
        jooqFile.setParent(new BigDecimal(this.parent));
        jooqFile.setAttachmentCode(this.attachmentCode);
        jooqFile.setJournalId(new BigDecimal(this.journalId));
        jooqFile.setOoFileId(new BigDecimal(this.ooFileId));
        jooqFile.setIsLegacyOnlineEditorFile(this.isLegacyOnlineEditorFile);
        jooqFile.setIsCkEditorFile(this.isCKEditorFile);
        jooqFile.setWrittenDate(this.writtenDate);
        jooqFile.setIsAsciidoc(this.isAsciidoc);
        jooqFile.setLocationIds(this.locationIds.stream().map(BigDecimal::new).toList());
        jooqFile.setTagIds(this.tags.stream().map(BigDecimal::new).toList());
        return jooqFile;
    }

    public static File fromJooqFile(JooqFile jooqFile) {
        File file = new File();
        file.setId(jooqFile.getId().intValue());
        file.setPath(jooqFile.getPath());
        file.setDate(jooqFile.getFileDate());
        file.setUuid(jooqFile.getUuid());
        file.setContent(jooqFile.getContent());
        file.setAnnotation(jooqFile.getAnnotation());
        file.setTitle(jooqFile.getTitle());
        file.setDescription(jooqFile.getDescription());
        if (jooqFile.getParent() != null) {
            file.setParent(jooqFile.getParent().intValue());
        }
        file.setAttachmentCode(jooqFile.getAttachmentCode());
        if (jooqFile.getJournalId() != null) {
            file.setJournalId(jooqFile.getJournalId().intValue());
        }
        if (jooqFile.getOoFileId() != null) {
            file.setOOFileId(jooqFile.getOoFileId().intValue());
        }
        file.setLegacyOnlineEditorFile(Boolean.TRUE.equals(jooqFile.getIsLegacyOnlineEditorFile()));
        file.setCKEditorFile(Boolean.TRUE.equals(jooqFile.getIsCkEditorFile()));
        file.setWrittenDate(jooqFile.getWrittenDate());
        file.setAsciidoc(Boolean.TRUE.equals(jooqFile.getIsAsciidoc()));
        System.out.println("JooqFile locationIds: " + jooqFile.getLocationIds());
        file.setLocationIds(jooqFile.getLocationIds().stream().map(BigDecimal::intValue).toList());
        return file;
    }

}
