package com.info25.journalindex.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.info25.generated.tables.pojos.Locations;

import jakarta.persistence.Transient;
import lombok.Data;

@Data
@Table("locations")
public class Location {
    @Id
    int id;
    double latitude;
    double longitude;
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
            this.latitude = Double.parseDouble(parts[0]);
            this.longitude = Double.parseDouble(parts[1]);
        }
    }

    public Location() {
    }

    public static Location fromJooqLocation(Locations jooqLocation) {
        Location location = new Location();
        location.setId(jooqLocation.getId().intValue());
        location.setLatitude(jooqLocation.getLatitude().doubleValue());
        location.setLongitude(jooqLocation.getLongitude().doubleValue());
        location.setAddress(jooqLocation.getAddress());
        location.setBuildingName(jooqLocation.getBuildingName());
        return location;
    }
}
