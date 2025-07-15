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


// this class is very similar to the HTMLAnnotator,
// I could consider merging them in the future
@RestController
@RequestMapping("/api/annotation/image")
public class ImageAnnotator {
    @Autowired
    FileRepository fileRepository;

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

    @PostMapping("/annotations/{id}")
    public String createAnnotation(@PathVariable("id") int id, @RequestBody JsonNode data) {
        ObjectNode dataNode = (ObjectNode) data;
        HTMLAnnotationDto existingData = getFileAnnotations(id);
        existingData.getAnnotations().put(dataNode.get("id").asText(), dataNode);
        saveFileAnnotations(id, existingData);

        return dataNode.toString();
    }

    @PutMapping("/annotations/{id}/{uuid}")
    public String updateAnnotation(@RequestBody JsonNode data, @PathVariable("uuid") String uuid, @PathVariable("id") int id) {
        ObjectNode dataNode = (ObjectNode) data;
        HTMLAnnotationDto existingData = getFileAnnotations(id);

        existingData.getAnnotations().put(uuid, dataNode);
        saveFileAnnotations(id, existingData);
        return dataNode.toString();

    }

    @DeleteMapping("/annotations/{id}/{uuid}")
    public String deleteAnnotation(@PathVariable("uuid") String uuid, @PathVariable("id") int id) {
        HTMLAnnotationDto existingData = getFileAnnotations(id);

        existingData.getAnnotations().remove(uuid);
        saveFileAnnotations(id, existingData);
        return "{}";
    }

    private HTMLAnnotationDto getFileAnnotations(int id) {
        File f = fileRepository.getWithoutSolr(id);
        if (f.getAnnotations() == null || f.getAnnotations().isEmpty()) {
            f.setAnnotations("{\"annotations\": {}}");
        }
        HTMLAnnotationDto dto = null;
        ObjectMapper om = new ObjectMapper();
        try {
            dto = om.readValue(f.getAnnotations(), HTMLAnnotationDto.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dto;
    }

    private void saveFileAnnotations(int id, HTMLAnnotationDto data) {
        File f = fileRepository.getById(id);
        ObjectMapper om = new ObjectMapper();
        
        
        try {
            f.setAnnotations(om.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        fileRepository.saveToSql(f);
    }

}
