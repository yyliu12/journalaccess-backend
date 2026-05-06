package com.info25.journalindex.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.info25.journalindex.apidtos.FileSearchDto;
import com.info25.journalindex.apidtos.FileSearchDtoMapper;
import com.info25.journalindex.models.Location;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.LocationRepository;
import com.info25.journalindex.util.PageableResponse;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/locations")
public class LocationCrud {
    @Autowired
    LocationRepository locationRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    FileSearchDtoMapper fileSearchDtoMapper;

    @PostMapping("create")
    public String create(@RequestParam("coordinates") String coordinates,
            @RequestParam("address") String address,
            @RequestParam("buildingName") String buildingName) {

        Location l = new Location();
        l.setCoordinates(coordinates);
        l.setAddress(address);
        l.setBuildingName(buildingName);

        locationRepository.save(l);

        return Integer.toString(l.getId());
    }

    @PostMapping("update")
    public String update(@RequestParam("coordinates") String coordinates,
            @RequestParam("address") String address,
            @RequestParam("buildingName") String buildingName,
            @RequestParam("id") int id) {
        Location l = locationRepository.findById(id);
        l.setCoordinates(coordinates);
        l.setAddress(address);
        l.setBuildingName(buildingName);
        locationRepository.updateSafe(l);

        return "ok";
    }

    @PostMapping("delete")
    public String deleteLocation(@RequestParam("id") int id) {
        locationRepository.deleteSafe(id);
        return "ok";
    }

    @PostMapping("getByIds")
    public Iterable<Location> getLocationsByIds(@RequestParam("ids") JsonNode ids) {
        ArrayList<Integer> locationIds = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            locationIds.add(ids.get(i).asInt());
        }
        return locationRepository.findByIdIn(locationIds);
    }

    @PostMapping("getById")
    public Location getLocationById(@RequestParam("id") int id) {
        return locationRepository.findById(id);
    }

    @PostMapping("getList")
    public PageableResponse<Location> getLocations(@RequestParam("page") int page) {
        Page<Location> data = locationRepository.findAllByOrderByIdDesc(Pageable.ofSize(20).withPage(page));
        PageableResponse<Location> response = new PageableResponse<>();
        response.setPages(data.getTotalPages());
        response.setData(data.getContent());
        return response;
    }

    @PostMapping("search")
    public List<Location> searchLocations(@RequestParam("query") String query) {
        return locationRepository.searchByBuildingNameOrAddress(query);
    }
}
