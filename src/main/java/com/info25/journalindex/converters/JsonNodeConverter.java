package com.info25.journalindex.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonNodeConverter implements Converter<String, JsonNode> {

    private final ObjectMapper objectMapper;

    public JsonNodeConverter() {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public JsonNode convert(String source) {
        try {
            return objectMapper.readTree(source);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert String to JsonNode", e);
        }
    }
    
}
