package com.info25.journalindex.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.info25.journalindex.models.Location;

public interface LocationRepository extends CrudRepository<Location, Integer>, LocationRepositoryCustom {
    Location findById(int id);
    List<Location> findByIdIn(List<Integer> ids);
    Page<Location> findAllByOrderByIdDesc(Pageable pageable);
    @Query("SELECT * FROM locations WHERE building_name ILIKE CONCAT('%', :query, '%') OR address ILIKE CONCAT('%', :query, '%') ORDER BY id DESC")
    List<Location> searchByBuildingNameOrAddress(@Param("query") String query);
}
