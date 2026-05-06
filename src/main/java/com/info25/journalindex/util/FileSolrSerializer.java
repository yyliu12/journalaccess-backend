package com.info25.journalindex.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.controllers.annotation.HTMLAnnotator;
import com.info25.journalindex.controllers.annotation.ImageAnnotator;
import com.info25.journalindex.controllers.annotation.PDFAnnotator;
import com.info25.journalindex.models.File;
import com.info25.journalindex.models.Location;
import com.info25.journalindex.repositories.EventFileRepository;
import com.info25.journalindex.repositories.LocationRepository;

/**
 * This class takes a File object and creates Solr modify query JSON
 */
@Component
public class FileSolrSerializer {
    @Autowired
    EventFileRepository eventFileRepository;

    @Autowired
    LocationRepository locationRepository;

    /**
     * Turns a file into a Solr modify query
     * @param f The file to create a Solr modify query for
     * @return The JSON data as a JsonNode
     */
    public JsonNode serializeForSolrModifyQuery(File f) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("id", f.getId());
        rootNode.set("date", createSet(mapper.valueToTree(DateUtils.localDateToTimestamp(f.getDate()))));
        rootNode.set("content", createSet(mapper.valueToTree(retrieveAllTextContent(f))));
        rootNode.set("location", createSet(mapper.valueToTree(locationRepository.findByIdIn(f.getLocationIds()).stream()
                .map(location -> location.getCoordinates())
                .toList()
        )));
        rootNode.set("events", createSet(mapper.valueToTree(
            eventFileRepository.findByFile(f.getId()).stream()
                .map(eventFile -> eventFile.getEvent())
                .toList()
        )));
        rootNode.set("tags", createSet(mapper.valueToTree(f.getTags())));
        rootNode.set("journal_id", mapper.valueToTree(f.getJournalId()));
        return rootNode;
    }

    public ObjectNode serializeEventsForSolrModifyQuery(File f) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("id", f.getId());
        rootNode.set("events", createSet(mapper.valueToTree(
            eventFileRepository.findByFile(f.getId()).stream()
                .map(eventFile -> eventFile.getEvent())
                .toList()
        )));
        return rootNode;
    }

    public ObjectNode serializeTagsForSolrModifyQuery(File f) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("id", f.getId());
        rootNode.set("tags", createSet(mapper.valueToTree(f.getTags())));
        return rootNode;
    }

    public ObjectNode serializeJournalsForSolrModifyQuery(File f) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("id", f.getId());
        rootNode.set("journal_id", createSet(mapper.valueToTree(f.getJournalId())));
        return rootNode;
    }

    public ObjectNode serializeLocationsForSolrModifyQuery(File f, HashMap<Integer, Location> locationCache) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("id", f.getId());
        ArrayList<String> coordinates = new ArrayList<>();

        ArrayList<Integer> locationIds = f.getLocationIds();
        ArrayList<Integer> toSearch = new ArrayList<>();
        for (Integer locationId : locationIds) {
            if (locationCache.containsKey(locationId)) {
                coordinates.add(locationCache.get(locationId).getCoordinates());
            } else {
                toSearch.add(locationId);
            }
        }

        for (Location location : locationRepository.findByIdIn(toSearch)) {
            coordinates.add(location.getCoordinates());
            locationCache.put(location.getId(), location);
        }

        rootNode.set("location", createSet(mapper.valueToTree(coordinates)));

        return rootNode;
    }


    /**
     * Gets all text content that could conceivably be searched into one
     * and the annotations.
     * @param f what file to get the text content from
     * @return a string containing all the text content
     */
    public static String retrieveAllTextContent(File f) {
        StringBuilder sb = new StringBuilder();

        sb.append(f.getContent())
                .append(" ");
        if (f.getAnnotation() != null && !f.getAnnotation().isEmpty()) {
            switch (ContentType.getFileExt(f.getPath())) {
                case "pdf":
                    sb.append(PDFAnnotator.getRawTextOfAnnotations(f.getAnnotation()));
                    break;
                case "jpg":
                case "jpeg":
                case "png":
                    sb.append(ImageAnnotator.annotationTextContent(f.getAnnotation()));
                    break;
                case "html":
                    sb.append(HTMLAnnotator.annotationTextContent(f.getAnnotation()));
                    break;
            }
        }
        
        sb.append(" ");
        if (f.getTitle() != null) {
            sb.append(f.getTitle()).append(" ");
        }
        if (f.getDescription() != null) {
            sb.append(f.getDescription()).append(" ");
        }

        return sb.toString();
    }

    /**
     * As Solr atomic updates require that the values be enclosed in an object
     * with the "set" key, this function is a helper function to create such an object
     * based on an already existing JsonNode.
     * @param node The JsonNode that will be the value of "set" in the resulting object
     * @return The node wrapped in an object with the "set" key
     */
    private static JsonNode createSet(JsonNode node) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode setNode = mapper.createObjectNode();
        setNode.set("set", node);
        return setNode;
    }
}
