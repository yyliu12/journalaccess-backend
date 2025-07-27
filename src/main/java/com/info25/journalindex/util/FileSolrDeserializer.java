package com.info25.journalindex.util;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.info25.journalindex.models.File;

/**
 * This class takes JSON data from Solr and creates a File object
 * from the JSON data.
 */
public class FileSolrDeserializer {
    /**
     * Deserializes a single file from a JSON doc.
     * @param doc The JSON document representing a file.
     * @return A File object with the JSON data
     */
    public static File deserializeSingle(JsonNode doc) {
        File f = new File();

        f.setUuid(doc.get("id").asText());
        f.setPath(doc.get("path").asText());
        f.setDate(DateUtils.timestampToLocalDate(doc.get("date").asLong()));

        // only some files have content & annotation_content
        if (doc.has("content"))
            f.setContent(doc.get("content").asText());
        if (doc.has("annotation_content"))
            f.setAnnotationContent(doc.get("annotation_content").asText());
        
        // Addresses & coordinates are stored as two different arrays; this
        // code combines them into a list of Location objects
        JsonNode addresses = doc.get("address");
        JsonNode coordinates = doc.get("location");
        if (addresses != null && coordinates != null && addresses.isArray() && coordinates.isArray()) {
            for (int i = 0; i < addresses.size(); i++) {
                String address = addresses.get(i).asText();
                String coordinate = coordinates.get(i).asText();
                f.addLocation(new File.Location(coordinate, address, ""));
            }
        }

        ArrayList<Integer> tags = new ArrayList<>();
        JsonNode tagsNode = doc.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tag : tagsNode) {
                tags.add(tag.asInt());
            }
        }
        f.setTags(tags);

        
        return f;
    }

    /**
     * Deserializes multiple files from a JsonNode array.
     * @param docs A JSON array with multiple documents
     * @return An ArrayList of File objects created from docs.
     */
    public static ArrayList<File> deserializeMany(JsonNode docs) {
        ArrayList<File> files = new ArrayList<>();
        for (JsonNode doc : docs) {
            files.add(deserializeSingle(doc));
        }
        return files;
    }
}
