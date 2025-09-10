package com.info25.journalindex.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.util.SolrSelectQuery;

/**
 * A helper class for interacting with the Solr database
 * 
 * This entire class is rather low-level
 */
@Component
public class SolrClient {
    String SOLR_URL;
    // the core name that is storing journal data
    String coreName;

    HttpClient client;

    public SolrClient(ConfigService configService) {
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        
        SOLR_URL = configService.getConfigOption("solrUrl");
        coreName = configService.getConfigOption("solrCoreName");
    }

    /**
     * Sends a request to the solr server. this is the most low-level function
     * @param url
     * @param postBody
     * @return
     */
    public String sendHttpRequest(String url, String postBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SOLR_URL + url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postBody))
                .build();

        HttpResponse<String> resp;
        try {
            resp = client.send(request, BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return resp.body();
    }

    /**
     * Sends a delete request with the following query
     * @param query a solr search query that specifies what files to delete
     */
    public void delete(String query) {
        ObjectMapper queryJson = new ObjectMapper();
        ObjectNode root = queryJson.createObjectNode();
        
        ObjectNode deleteNode = root.putObject("delete");
        deleteNode.put("query", query);
        
        String response = sendHttpRequest("/solr/" + coreName + "/update?commit=true", root.toString());
    }

    /**
     * sends a modify command 
     * @param json the json of the modify command
     */
    public void modify(JsonNode json) {
        String response = sendHttpRequest("/solr/" + coreName + "/update?commit=true", json.toString());
    }

    /**
     * Sends a select, or search command
     * @param q a SolrSelectQuery specifying what to search for
     * @return the json returend by solr
     */
    public JsonNode select(SolrSelectQuery q) {
        ObjectMapper queryJson = new ObjectMapper();
        ObjectNode root = queryJson.createObjectNode();
        
        ObjectNode query = root.putObject("params");

        query.put("q", q.getQ());
        query.put("fl", q.getFl());
        query.put("sort", q.getSort());
        query.put("rows", q.getRows());
        query.put("start", q.getStart());
        query.put("hl", q.getHl());
        query.put("hl.fl", q.getHlFl());
        query.put("fq", q.getFq());
        query.put("wt", "json");
        query.put("q.op", "AND");
        query.put("hl.bs.type", "WORD");
        query.put("hl.fragsize", 300);

        String response = sendHttpRequest("/solr/" + coreName + "/select", root.toString());
        ObjectMapper responseJson = new ObjectMapper();
        JsonNode responseRoot;
        try {
            responseRoot = responseJson.readTree(response);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return responseRoot;
    }


}
