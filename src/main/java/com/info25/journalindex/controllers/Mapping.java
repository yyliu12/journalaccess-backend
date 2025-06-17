package com.info25.journalindex.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.info25.journalindex.models.File.Location;
import com.info25.journalindex.services.TomTomService;

@RestController
public class Mapping {
    @Autowired
    TomTomService tomTomService;

    @PostMapping("/api/mapping/search")
    public List<Location> searchLocations(@RequestParam("query") String query) {
        List<Location> locations = tomTomService.searchForLocations(query);
        return locations;
    }
}
