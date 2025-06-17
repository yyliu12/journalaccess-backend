package com.info25.journalindex.util;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.info25.journalindex.models.File;

public class FileSolrDeserializer {
    public static File deserializeSingle(JsonNode doc) {
        File f = new File();

        f.setUuid(doc.get("id").asText());
        f.setPath(doc.get("path").asText());
        if (doc.has("content"))
            f.setContent(doc.get("content").asText());
        f.setDate(DateUtils.timestampToLocalDate(doc.get("date").asLong()));
        
        JsonNode addresses = doc.get("address");
        JsonNode coordinates = doc.get("location");
        if (addresses != null && coordinates != null && addresses.isArray() && coordinates.isArray()) {
            for (int i = 0; i < addresses.size(); i++) {
                String address = addresses.get(i).asText();
                String coordinate = coordinates.get(i).asText();
                f.addLocation(new File.Location(coordinate, address));
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

    public static ArrayList<File> deserializeMany(JsonNode docs) {
        ArrayList<File> files = new ArrayList<>();
        for (JsonNode doc : docs) {
            files.add(deserializeSingle(doc));
        }
        return files;
    }
}
