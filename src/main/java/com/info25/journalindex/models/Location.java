package com.info25.journalindex.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.persistence.Transient;
import lombok.Data;

@Data
@Table("locations")
public class Location {
    @Id
    int id;
    float latitude;
    float longitude;
    String address;
    String buildingName;

    public Location(String coordinates, String address, String buildingName) {
        this.address = address;
        this.buildingName = buildingName;
    }

    public String getCoordinates() {
        return latitude + "," + longitude;
    }

    public void setCoordinates(String coordinates) {
        String[] parts = coordinates.split(",");
        if (parts.length == 2) {
            this.latitude = Float.parseFloat(parts[0]);
            this.longitude = Float.parseFloat(parts[1]);
        }
    }

    public Location() {
    }
}
