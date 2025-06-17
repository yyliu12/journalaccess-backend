package com.info25.journalindex.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.models.File;

public class FileSolrSerializer {
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
        return rootNode;
    }

    private static JsonNode createSet(JsonNode node) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode setNode = mapper.createObjectNode();
        setNode.set("set", node);
        return setNode;
    }
}
