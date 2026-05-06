package com.info25.journalindex.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

@Data
@Table("locations")
public class Location {
    @Id
    int id;
    String coordinates;
    String address;
    String buildingName;

    public Location(String coordinates, String address, String buildingName) {
        this.coordinates = coordinates;
        this.address = address;
        this.buildingName = buildingName;
    }

    public Location() {
    }
}
