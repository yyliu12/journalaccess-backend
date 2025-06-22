package com.info25.journalindex.controllers.annotation;

import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class HTMLAnnotationDto {
    private HashMap<String, JsonNode> annotations;
}
