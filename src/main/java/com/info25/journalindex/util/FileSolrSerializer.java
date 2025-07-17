package com.info25.journalindex.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.models.File;

/**
 * This class takes a File object and creates Solr modify query JSON
 */
public class FileSolrSerializer {
    /**
     * Turns a file into a Solr modify query
     * @param f The file to create a Solr modify query for
     * @return The JSON data as a JsonNode
     */
    public static JsonNode serializeForSolrModifyQuery(File f) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("id", f.getUuid());
        rootNode.set("path", createSet(mapper.valueToTree(f.getPath())));
        rootNode.set("date", createSet(mapper.valueToTree(DateUtils.localDateToTimestamp(f.getDate()))));
        rootNode.set("content", createSet(mapper.valueToTree(f.getContent())));
        rootNode.set("address", createSet(mapper.valueToTree(f.getLocations().stream()
                .map(File.Location::getAddress)
                .toList())));
        rootNode.set("location", createSet(mapper.valueToTree(f.getLocations().stream()
                .map(File.Location::getCoordinate)
                .toList())));
        rootNode.set("tags", createSet(mapper.valueToTree(f.getTags())));
        rootNode.set("annotation_content", createSet(mapper.valueToTree(f.getAnnotationContent())));
        return rootNode;
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
