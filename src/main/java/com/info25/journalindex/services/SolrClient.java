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

@Component
public class SolrClient {
    final String SOLR_URL = "http://127.0.0.1:8983";
    final String coreName = "journal";

    HttpClient client;

    public SolrClient() {
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

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

    public void delete(String query) {
        ObjectMapper queryJson = new ObjectMapper();
        ObjectNode root = queryJson.createObjectNode();
        
        ObjectNode deleteNode = root.putObject("delete");
        deleteNode.put("query", query);
        
        String response = sendHttpRequest("/solr/" + coreName + "/update?commit=true", root.toString());
        System.out.println(response);
    }

    public void modify(JsonNode json) {
        System.out.println(json.toString());
        String response = sendHttpRequest("/solr/" + coreName + "/update?commit=true", json.toString());
        System.out.println(response);
    }

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

        String response = sendHttpRequest("/solr/" + coreName + "/select", root.toString());
        System.out.println(response);
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
