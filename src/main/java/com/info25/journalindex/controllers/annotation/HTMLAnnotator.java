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

@RestController
@RequestMapping("/api/annotation/html")
public class HTMLAnnotator {
    @Autowired
    FileRepository fileRepository;

    @GetMapping("/search")
    public String search(@RequestParam("id") String id) throws JsonProcessingException {
        HTMLAnnotationDto data = getFileAnnotations(Integer.parseInt(id));
        ObjectMapper om = new ObjectMapper();

        ObjectNode result = om.createObjectNode();
        result.put("rows", data.getAnnotations().size());
        ArrayNode annotations = result.putArray("rows");

        for (JsonNode node : data.getAnnotations().values()) {
            annotations.add(node);
        }
        return om.writeValueAsString(result);
    }

    @PostMapping("/annotations")
    public String createAnnotation(@RequestBody JsonNode data) {
        ObjectNode dataNode = (ObjectNode) data;
        dataNode.put("id", UUID.randomUUID().toString());
        int id = dataNode.get("fileId").asInt();
        dataNode.remove("fileId");

        HTMLAnnotationDto existingData = getFileAnnotations(id);
        existingData.getAnnotations().put(dataNode.get("id").asText(), dataNode);
        saveFileAnnotations(id, existingData);

        return dataNode.toString();
    }

    @PutMapping("/annotations/{uuid}")
    public String updateAnnotation(@RequestBody JsonNode data, @PathVariable("uuid") String uuid) {
        ObjectNode dataNode = (ObjectNode) data;
        int id = dataNode.get("fileId").asInt();
        dataNode.remove("fileId");

        HTMLAnnotationDto existingData = getFileAnnotations(id);

        existingData.getAnnotations().put(uuid, dataNode);
        saveFileAnnotations(id, existingData);
        return dataNode.toString();

    }

    @DeleteMapping("/annotations/{uuid}")
    public String deleteAnnotation(@RequestBody JsonNode data, @PathVariable("uuid") String uuid) {
        ObjectNode dataNode = (ObjectNode) data;
        int id = dataNode.get("fileId").asInt();
        dataNode.remove("fileId");

        HTMLAnnotationDto existingData = getFileAnnotations(id);

        existingData.getAnnotations().remove(uuid);
        saveFileAnnotations(id, existingData);
        return dataNode.toString();
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
