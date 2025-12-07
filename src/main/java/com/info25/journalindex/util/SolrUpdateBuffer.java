package com.info25.journalindex.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class SolrUpdateBuffer {
    ArrayNode updateBuffer = JsonNodeFactory.instance.arrayNode();
    
    public SolrUpdateBuffer() {}

    public void addToBuffer(JsonNode node) {
        updateBuffer.add(node);
    }

    public ArrayNode getUpdateBuffer() {
        return updateBuffer;
    }
}
