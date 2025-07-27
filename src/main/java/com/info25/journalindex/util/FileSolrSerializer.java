package com.info25.journalindex.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.controllers.annotation.ImageAnnotator;
import com.info25.journalindex.controllers.annotation.PDFAnnotator;
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
        rootNode.put("id", f.getId());
        rootNode.set("date", createSet(mapper.valueToTree(DateUtils.localDateToTimestamp(f.getDate()))));
        rootNode.set("content", createSet(mapper.valueToTree(retrieveAllTextContent(f))));
        rootNode.set("location", createSet(mapper.valueToTree(f.getLocations().stream()
                .map(File.Location::getCoordinate)
                .toList())));
        rootNode.set("tags", createSet(mapper.valueToTree(f.getTags())));
        return rootNode;
    }

    /**
     * Gets all text content that could conceivably be searched into one
     * string. this includes the actual content of the file (field: content)
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
                    sb.append(f.getAnnotationContent());
                    break;
            }
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
