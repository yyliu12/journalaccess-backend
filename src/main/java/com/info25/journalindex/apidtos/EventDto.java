package com.info25.journalindex.apidtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventDto {
    int id;
    String name;
    int parent;
    private boolean isFolder;
    String description;
    boolean hasChildren;

    @JsonProperty("isFolder") // otherwise jackson cuts off "is"
    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }
}
