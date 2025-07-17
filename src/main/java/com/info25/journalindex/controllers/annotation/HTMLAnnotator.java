package com.info25.journalindex.controllers.annotation;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;

/**
 * This file handles annotations for HTML files.
 * 
 * We employ the Annotator.js library.
 */
@RestController
@RequestMapping("/api/annotation/html")
public class HTMLAnnotator {
    @Autowired
    FileRepository fileRepository;

    /**
     * This endpoint returns the annotations for an HTML files.
     * @param id The id of the file
     * @return A JSON string with annotation data
     * @throws JsonProcessingException
     */
    @GetMapping("/search")
    public String search(@RequestParam("id") String id) throws JsonProcessingException {
        HTMLAnnotationDto data = getFileAnnotations(Integer.parseInt(id));
        ObjectMapper om = new ObjectMapper();

        // annotator.js requires that annotations be returned as an object
        // with the key "rows," containing an array of annotation objects
        ObjectNode result = om.createObjectNode();
        result.put("rows", data.getAnnotations().size());
        ArrayNode annotations = result.putArray("rows");

        for (JsonNode node : data.getAnnotations().values()) {
            annotations.add(node);
        }

        return om.writeValueAsString(result);
    }

    /**
     * This endpoint creates a new annotation.
     * @param data The JSON data of the annotation.
     * @return A JSON string with the created annotation data.
     */
    @PostMapping("/annotations")
    public String createAnnotation(@RequestBody JsonNode data) {
        ObjectNode dataNode = (ObjectNode) data;
        // We create a new UUID since Annotator.js does not do so for us.
        dataNode.put("id", UUID.randomUUID().toString());
        int id = dataNode.get("fileId").asInt();
        // Do not save the fileId in the annotation data; that is only
        // for the client to inform the server which file the annotation
        // should be saved to.
        dataNode.remove("fileId");

        HTMLAnnotationDto existingData = getFileAnnotations(id);
        existingData.getAnnotations().put(dataNode.get("id").asText(), dataNode);
        saveFileAnnotations(id, existingData);

        return dataNode.toString();
    }
    /**
     * This endpoint updates an existing annotation.
     * @param data The JSON data of the annotation.
     * @param uuid The UUID of the annotation to update.
     * @return A JSON string with the updated annotation data.
     */
    @PutMapping("/annotations/{uuid}")
    public String updateAnnotation(@RequestBody JsonNode data, @PathVariable("uuid") String uuid) {
        ObjectNode dataNode = (ObjectNode) data;
        int id = dataNode.get("fileId").asInt();
        dataNode.remove("fileId");

        HTMLAnnotationDto existingData = getFileAnnotations(id);

        // Updates the annotation map with the new data for the given uuid
        existingData.getAnnotations().put(uuid, dataNode);
        saveFileAnnotations(id, existingData);
        return dataNode.toString();

    }

    /**
     * This endpoint deletes an annotation.
     * @param data The JSON data of the annotation to delete.
     * @param uuid The UUID of the annotation to delete.
     * @return A JSON string with the deleted annotation data.
     */
    @DeleteMapping("/annotations/{uuid}")
    public String deleteAnnotation(@RequestBody JsonNode data, @PathVariable("uuid") String uuid) {
        ObjectNode dataNode = (ObjectNode) data;
        int id = dataNode.get("fileId").asInt();
        dataNode.remove("fileId");

        HTMLAnnotationDto existingData = getFileAnnotations(id);

        // Removes the annotation with the given uuid from the annotations object
        existingData.getAnnotations().remove(uuid);
        saveFileAnnotations(id, existingData);
        return dataNode.toString();
    }

    /**
     * Gets annotation data for a file.
     * @param id The id of the file to get annotations for.
     * @return An HTMLAnnotationDto object with the annotations.
     */
    private HTMLAnnotationDto getFileAnnotations(int id) {
        // No Solr data is needed here -- annotations are stored in the SQL db.
        File f = fileRepository.getWithoutSolr(id);
        // We populate an empty annotations dictionary if there are no annotations
        if (f.getAnnotations() == null || f.getAnnotations().isEmpty()) {
            f.setAnnotations("{\"annotations\": {}}");
        }
        HTMLAnnotationDto dto = null;
        ObjectMapper om = new ObjectMapper();
        try {
            dto = om.readValue(f.getAnnotations(), HTMLAnnotationDto.class);
        } catch (Exception e) { }
        return dto;
    }

    /**
     * Saves the annotations for a file. Also generates annotation content for searching.
     * @param id The id of the file to save annotations for.
     * @param data The annotation data to save.
     */
    private void saveFileAnnotations(int id, HTMLAnnotationDto data) {
        File f = fileRepository.getById(id);
        ObjectMapper om = new ObjectMapper();
        
        StringBuilder annotationContent = new StringBuilder();
        for (JsonNode node : data.getAnnotations().values()) {
            // Accumulates the text of all annotations
            annotationContent.append(" ").append(node.get("text").asText());
        }
        f.setAnnotationContent(annotationContent.toString());

        try {
            f.setAnnotations(om.writeValueAsString(data));
        } catch (JsonProcessingException e) { }

        fileRepository.save(f);
    }

}
