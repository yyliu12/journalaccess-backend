package com.info25.journalindex.repositories;

import com.info25.journalindex.models.Location;

public interface LocationRepositoryCustom {
    void deleteSafe(int locationId);
    void updateSafe(Location location);
}
