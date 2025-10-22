package com.info25.journalindex.controllers;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.info25.journalindex.apidtos.*;
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

/**
 * Collection of functions responsible for searching, that is, discovering files
 */
@RestController
public class Search {
    // the start & end search dates for the on this day function
    final int OTD_START_YEAR = 2022;
    final int OTD_END_YEAR = 2050;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    BacklinkRepository backlinkRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    FileSearchDtoMapper fileSearchDtoMapper;

	/**
	 * Main searching function used for discovering files on the search and map screen
     * 
     * query is converted to a SearchOptions object
     * page notates which page of results to get -- this is ignored when a boundsQuery is specified
     *      as pagination does not make sense on the map screen
     * boundsQuery specifies a specific bounding box for the location property -- if this is specified
     *      this is being queried from the map screen
	 */
    @PostMapping("/api/files/search")
    public SearchResponseDto search(@RequestParam(name = "query", required = false) JsonNode query,
                                   @RequestParam(name = "page", defaultValue = "0") int page,
                                   @RequestParam(name = "bounds", required = false) JsonNode boundsQuery) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        SearchOptions so = mapper.convertValue(query, SearchOptions.class);

        TagSearchOptions tagSearchOptions = so.getTags();
        String searchQuery = so.getQuery();

        SolrSelectQuery selectQuery = new SolrSelectQuery()
                .setHl("true")
                .setStart(page * 10)
                .setHlFl("content")
                .setSort(so.getSort());
        SolrQueryAssembler assembler = new SolrQueryAssembler();
        // if there is a bound query
        if (boundsQuery != null) {
            assembler.addTerm("location: [" +
                    boundsQuery.get("boundsSwLat").asDouble() + "," +
                    boundsQuery.get("boundsSwLng").asDouble() + " TO " +
                    boundsQuery.get("boundsNeLat").asDouble() + "," +
                    boundsQuery.get("boundsNeLng").asDouble() + "]");
            // 2147 is the max number of results that Solr can contain --
            // we want to return this many results because we pagination doesn't
            // exist on the map screen
            selectQuery.setRows(2147483647);
            // we don't need highlights data on the map screen because there
            // is no way to display it
            selectQuery.setHl("false");
        }

        if (tagSearchOptions.getTags().size() > 0) {
            Set<Integer> tagIds = new HashSet<Integer>(tagSearchOptions.getTags());
            // for each tag get and add the tag id to a list
            for (Integer tag : tagSearchOptions.getTags()) {
                // if we're recursively searching let's also call the find recursively in the tag repository
                if (tagSearchOptions.isRecursivelySearch()) {
                    tagIds.addAll(tagRepository.findRecursively(tag, tagSearchOptions.isIncludeFolders())
                            .stream().map(t -> t.getId()).toList());
                }
                tagIds.add(tag);
            }
            // create a solr query using the combining term
            assembler.addTerm("tags: (" + String.join(" " + tagSearchOptions.getCombiningTerm() + " ",
                    tagIds.stream().map(String::valueOf).toList()) + ")");
        }

        // if there are events selected, get the event ids and add them to a list
        if (so.getEvents() != null && so.getEvents().size() > 0) {
            assembler.addTerm("events: (" + String.join(" OR ", so.getEvents().stream()
                    .map(String::valueOf).toList()) + ")");
        }
        // date filtering
        if (so.isDateFilteringEnabled()) {
            assembler.addTerm("date: [" +
                    DateUtils.localDateToTimestamp(so.getStartDate()) + " TO " +
                    DateUtils.localDateToTimestamp(so.getEndDate()) + "]");
        }

        selectQuery.setFq(assembler.getFullQuery());

        if (searchQuery != "") {
            selectQuery.setQ("content:(" + searchQuery + ")");
        }

        SolrFileResponse dbResp = fileRepository.solrQueryForApi(selectQuery);

        SearchResponseDto resp = new SearchResponseDto();

        resp.setError(dbResp.getError());

        if (dbResp.getError() == null) {
            resp.setNumFound(dbResp.getNumFound());
            List<FileSearchDto> results = new ArrayList<>();
            JsonNode highlights = dbResp.getHighlights();
            // if we have highlights
            if (highlights != null) {
                for (File f : dbResp.getFiles()) {
                    JsonNode highlight = highlights.get(String.valueOf(f.getId())).get("content"); // all solr ids are strings
                    // there may be a highlights object but each highlight for each file
                    // may be null. this happens when searching by tags, for example --
                    // there is nothing for solr to highlight

                    if (highlight != null) {
                        FileSearchDto dto = fileSearchDtoMapper.toDtoWithHighlight(f,
                                highlight.get(0).asText());
                        results.add(dto);
                    } else {
                        FileSearchDto dto = fileSearchDtoMapper.toDto(f);
                        results.add(dto);
                    }
                }
            } else {
                for (File f : dbResp.getFiles()) {
                    results.add(fileSearchDtoMapper.toDto(f));
                }
            }
            resp.setFiles(results);
        }



        return resp;
    }
    /**
     * Gets files by date
     * @param date the date to get files for
     * @return a list of files on the specified date
     */
    @PostMapping("/api/files/byDate")
    public List<FileSearchDto> byDate(@RequestParam("date") String date) {
        LocalDate dateTime = DateUtils.parseFromString(date);
        List<File> results = fileRepository.getFilesByDate(dateTime);

        return fileSearchDtoMapper.toDtoList(results);
    }

    /**
     * Retrieves a specific file by id
     * @param id the id of the file to retrieve
     * @return the file data
     */
    @PostMapping("/api/files/byId")
    public File getFile(@RequestParam("id") int id) {
        File f = fileRepository.getById(id);
        System.out.println("Date: " + f.getDate());
        return f;
    }

    /**
     * Retrieves dates with files in the specific month and year
     * @param month the month to get dates with files for
     * @param year the year to get dates with files for
     * @return a list of dates with files
     */
    @PostMapping("/api/files/datesWithFiles")
    public List<LocalDate> datesWithFiles(@RequestParam("month") int month,
                                          @RequestParam("year") int year) {
        return fileRepository.getDatesWithFiles(month, year);
    }

    /**
     * Gets files with the same month and day
     * @param date the date to get files with the same month and day for
     * @return a list of files with the same month and day
     */
    @PostMapping("/api/files/onThisDate")
    public List<FileSearchDto> onThisDate(@RequestParam("date") String date) {
        LocalDate dateTime = DateUtils.parseFromString(date);
        ArrayList<FileSearchDto> results = new ArrayList<>();

        for (int year = OTD_START_YEAR; year <= OTD_END_YEAR; year++) {
            results.addAll(
                    fileSearchDtoMapper.toDtoList(
                            fileRepository.getFilesByDate(
                                    LocalDate.of(year, dateTime.getMonth(),
                                            dateTime.getDayOfMonth()
                                    )
                            )
                    )
            );
        }

        return results;
    }

    /**
     * gets attachments for a file id
     * @param id the id of the file
     * @return a list of file data, including the file itself as well file data for its attachments
     */
    @PostMapping("/api/files/getAttachments")
    public List<FileSearchDto> getAttachments(@RequestParam("id") int id) {
        List<File> files = fileRepository.getAttachmentsAndFile(id);
        List<FileSearchDto> dtos = new ArrayList<>();
        for (File f : files) {
            dtos.add(fileSearchDtoMapper.toDto(f));
        }
        return dtos;
    }

    /**
     * Get files by month and year
     */
    @PostMapping("/api/files/byMonthAndYear")
    public List<FileSearchDto> getByMonthAndYear(@RequestParam("month") int month, 
                                                 @RequestParam("year") int year) {
        YearMonth ym = YearMonth.of(year, month);
        List<File> files = fileRepository.getFilesByDateRange(ym.atDay(1), ym.atEndOfMonth());

        return fileSearchDtoMapper.toDtoList(files);
    }
}
