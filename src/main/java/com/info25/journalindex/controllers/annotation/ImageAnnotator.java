package com.info25.journalindex.controllers.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;


/**
 * This file handles annotations for image files.
 * 
 * We use the annotorious seadragon library.
 */
@RestController
@RequestMapping("/api/annotation/image")
public class ImageAnnotator {
    @Autowired
    FileRepository fileRepository;

    /**
     * Returns annotations for the image file with given id.
     * @param id The id of the file
     * @return JSON data containing the annotation data
     * @throws JsonProcessingException
     */
    @GetMapping("/annotations/{id}")
    public String search(@PathVariable("id") String id) throws JsonProcessingException {
        HTMLAnnotationDto data = getFileAnnotations(Integer.parseInt(id));
        ObjectMapper om = new ObjectMapper();

        ArrayNode annotations = om.createArrayNode();

        for (JsonNode node : data.getAnnotations().values()) {
            annotations.add(node);
        }
        return om.writeValueAsString(annotations);
    }

    /**
     * Creates an annotation on the given image file with the given data.
     * @param id The id of the file
     * @param data The annotation data
     * @return A JSON string with the created annotation data
     */
    @PostMapping("/annotations/{id}")
    public String createAnnotation(@PathVariable("id") int id, @RequestBody JsonNode data) {
        ObjectNode dataNode = (ObjectNode) data;
        HTMLAnnotationDto existingData = getFileAnnotations(id);
        // BTW Annotorious seadragon creates a random uuid for us
        existingData.getAnnotations().put(dataNode.get("id").asText(), dataNode);
        saveFileAnnotations(id, existingData);

        return dataNode.toString();
    }

    /**
     * Updates an existing annotation on the image file.
     * @param data The JSON data of the annotation to update.
     * @param uuid The UUID of the annotation to update.
     * @param id The id of the file
     * @return A JSON string with the updated annotation data.
     */
    @PutMapping("/annotations/{id}/{uuid}")
    public String updateAnnotation(@RequestBody JsonNode data, @PathVariable("uuid") String uuid, @PathVariable("id") int id) {
        ObjectNode dataNode = (ObjectNode) data;
        HTMLAnnotationDto existingData = getFileAnnotations(id);

        existingData.getAnnotations().put(uuid, dataNode);
        saveFileAnnotations(id, existingData);
        return dataNode.toString();

    }

    /**
     * Deletes an annotation from the image file.
     * @param uuid The UUID of the annotation to delete.
     * @param id The id of the file
     * @return an empty json object.
     */
    @DeleteMapping("/annotations/{id}/{uuid}")
    public String deleteAnnotation(@PathVariable("uuid") String uuid, @PathVariable("id") int id) {
        HTMLAnnotationDto existingData = getFileAnnotations(id);

        existingData.getAnnotations().remove(uuid);
        saveFileAnnotations(id, existingData);
        return "{}";
    }

    /**
     * Gets annotation data for a file.
     * @param id The id of the file to get annotations for.
     * @return An HTMLAnnotationDto object with the annotations.
     */
    private HTMLAnnotationDto getFileAnnotations(int id) {
        File f = fileRepository.getWithoutSolr(id);
        // If there are no annotations, we create an empty annotations object
        if (f.getAnnotation() == null || f.getAnnotation().isEmpty()) {
            f.setAnnotation("{\"annotations\": {}}");
        }
        HTMLAnnotationDto dto = null;
        ObjectMapper om = new ObjectMapper();
        try {
            dto = om.readValue(f.getAnnotation(), HTMLAnnotationDto.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dto;
    }

    /**
     * Saves the annotations for a file. Also generates annotation content for searching.
     * @param id The id of the file
     * @param data The annotation data to save
     */
    private void saveFileAnnotations(int id, HTMLAnnotationDto data) {
        File f = fileRepository.getById(id);
        ObjectMapper om = new ObjectMapper();

        try {
            f.setAnnotation(om.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        fileRepository.save(f);
    }

    /**
     * Gets the raw text of the annotation
     * @return raw text of all annotations
     */
    public static String annotationTextContent(String data) {
        ObjectMapper om = new ObjectMapper();
        HTMLAnnotationDto dto = null;

        try {
            dto = om.readValue(data, HTMLAnnotationDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        /**
         * Example annotorious seadragon annotation:
         *
         * {
         *   "body": [
         *     { "value": "Comment 1" },
         *     { "value": "Comment 2" }
         *   ]
         * }
         */
        StringBuilder annotationContent = new StringBuilder();
        for (JsonNode node : dto.getAnnotations().values()) {
            for (JsonNode body : node.get("body")) {
                // Accumulates the text of all bodies
                annotationContent.append(" ").append(body.get("value").asText());
            }
        }

        return annotationContent.toString();
    }

}
