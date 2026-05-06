package com.info25.journalindex.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.info25.journalindex.models.Location;

public class LocationRepositoryCustomImpl implements LocationRepositoryCustom {
    @Autowired
    @Lazy
    FileRepository fileRepository;

    @Lazy
    @Autowired
    LocationRepository locationRepository;

    @Override
    public void deleteSafe(int locationId) {
        fileRepository.deleteLocationFromFiles(locationId);
        locationRepository.deleteById(locationId);
    }

    @Override
    public void updateSafe(Location location) {
        locationRepository.save(location);
        fileRepository.updateFilesWithLocation(location.getId());
    }
}
