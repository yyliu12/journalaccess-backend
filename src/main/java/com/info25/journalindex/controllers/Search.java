package com.info25.journalindex.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.info25.journalindex.apidtos.FileSearchDto;
import com.info25.journalindex.apidtos.SearchOptions;
import com.info25.journalindex.apidtos.TagSearchOptions;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.SolrQueryAssembler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.BacklinkRepository;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.TagRepository;
import com.info25.journalindex.repositories.FileRepository.SolrFileResponse;
import com.info25.journalindex.util.SolrSelectQuery;

@RestController
public class Search {
    final int OTD_START_YEAR = 2022;
    final int OTD_END_YEAR = 2050;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    BacklinkRepository backlinkRepository;

    @Autowired
    TagRepository tagRepository;

    @PostMapping("/api/files/search")
    public SolrFileResponse search(@RequestParam(name = "query", required = false) JsonNode query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "bounds", required = false) JsonNode boundsQuery) {

        ObjectMapper mapper = new ObjectMapper();
        SearchOptions so = mapper.convertValue(query, SearchOptions.class);

        TagSearchOptions tagSearchOptions = so.getTags();
        String searchQuery = so.getQuery();

        SolrSelectQuery selectQuery = new SolrSelectQuery()
            .setHl("true")
            .setStart(page * 10)
            .setHlFl("content")
            .setSort(so.getSort());
        SolrQueryAssembler assembler = new SolrQueryAssembler();
        if (boundsQuery != null) {

            assembler.addTerm("location: [" +
                    boundsQuery.get("boundsSwLat").asDouble() + "," +
                    boundsQuery.get("boundsSwLng").asDouble() + " TO " +
                    boundsQuery.get("boundsNeLat").asDouble() + "," +
                    boundsQuery.get("boundsNeLng").asDouble() + "]");
            selectQuery.setRows(2147483647);
            selectQuery.setHl("false");
        }

        if (tagSearchOptions.getTags().size() > 0) {
            Set<Integer> tagIds = new HashSet<Integer>(tagSearchOptions.getTags());
            for (Integer tag : tagSearchOptions.getTags()) {
                if (tagSearchOptions.isRecursivelySearch()) {
                    tagIds.addAll(tagRepository.findRecursively(tag, tagSearchOptions.isIncludeFolders())
                            .stream().map(t -> t.getId()).toList());
                }
                tagIds.add(tag);
            }

            assembler.addTerm("tags: (" + String.join(" " + tagSearchOptions.getCombiningTerm() + " ",
                    tagIds.stream().map(String::valueOf).toList()) + ")");
        }

        selectQuery.setFq(assembler.getFullQuery());

        if (searchQuery != "") {
            selectQuery.setQ(searchQuery);
        }

        SolrFileResponse resp = fileRepository.solrQueryForApi(selectQuery);

        for (FileSearchDto f : resp.getFiles()) {
            backlinkRepository.populateBacklinks(f);
        }

        return resp;
    }

    @PostMapping("/api/files/byDate")
    public List<FileSearchDto> byDate(@RequestParam("date") String date) {
        LocalDate dateTime = DateUtils.parseFromString(date);
        List<FileSearchDto> results = fileRepository.getFilesByDateForApi(dateTime);
        for (FileSearchDto f : results) {
            backlinkRepository.populateBacklinks(f);
        }

        return results;
    }

    @PostMapping("/api/files/byId")
    public File getFile(@RequestParam("id") int id) {
        File f = fileRepository.getById(id);
        fileRepository.loadFromSolr(f);
        System.out.println("Date: " + f.getDate());
        return f;
    }

    @PostMapping("/api/files/datesWithFiles")
    public List<LocalDate> datesWithFiles(@RequestParam("month") int month,
            @RequestParam("year") int year) {
        return fileRepository.getDatesWithFiles(month, year);
    }

    @PostMapping("/api/files/onThisDate")
    public List<FileSearchDto> onThisDate(@RequestParam("date") String date) {
        LocalDate dateTime = DateUtils.parseFromString(date);
        ArrayList<FileSearchDto> results = new ArrayList<>();

        for (int year = OTD_START_YEAR; year <= OTD_END_YEAR; year++) {
            results.addAll(fileRepository
                    .getFilesByDateForApi(LocalDate.of(year, dateTime.getMonth(), dateTime.getDayOfMonth())));
        }

        for (FileSearchDto f : results) {
            backlinkRepository.populateBacklinks(f);
        }
        return results;
    }

}
