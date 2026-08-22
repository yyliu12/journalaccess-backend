package com.info25.journalindex.repositories;

import java.util.List;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.info25.generated.tables.pojos.Locations;
import com.info25.journalindex.models.Location;
import static com.info25.generated.tables.Locations.LOCATIONS;

public class LocationRepositoryCustomImpl implements LocationRepositoryCustom {
    @Autowired
    @Lazy
    FileRepository fileRepository;

    @Lazy
    @Autowired
    LocationRepository locationRepository;

    @Autowired
    DSLContext dsl;

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

    @Override
    public List<Location> searchByBuildingNameOrAddress(String query) {
        return dsl.selectFrom(LOCATIONS)
                .where(LOCATIONS.BUILDING_NAME.likeIgnoreCase("%" + query + "%")
                        .or(LOCATIONS.ADDRESS.likeIgnoreCase("%" + query + "%")))
                .fetchInto(Locations.class)
                .stream()
                .map(x -> Location.fromJooqLocation(x))
                .collect(Collectors.toList());
    }
}
