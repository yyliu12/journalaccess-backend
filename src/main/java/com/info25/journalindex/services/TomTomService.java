package com.info25.journalindex.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.info25.journalindex.models.File.Location;

@Service
public class TomTomService {
    String TOMTOM_APIKEY;

    HttpClient client;

    public TomTomService(ConfigService configService) {
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        
        TOMTOM_APIKEY = configService.getConfigOption("tomtomApiKey");
    }

    public ArrayList<Location> searchForLocations(String query) {
        query = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.tomtom.com/search/2/search/" + query + ".json?key=" + TOMTOM_APIKEY +
                "&relatedPois=off";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp;

        try {
            resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        String body = resp.body();

        System.out.println(body);

        ObjectMapper mapper = new ObjectMapper();

        ArrayList<Location> locations = new ArrayList<>();
        try {
            JsonNode results = mapper.readTree(body).get("results");
            for (JsonNode result : results) {
                StringBuilder address = new StringBuilder();
                if (result.has("poi")) {
                    address.append(result.get("poi").get("name").asText());
                }
                address.append(", ");
                address.append(result.get("address").get("freeformAddress").asText());
                Location location = new Location(
                        result.get("position").get("lat").asText() + ", " + result.get("position").get("lon").asText(),
                        address.toString(),
                        ""
                );
                locations.add(location);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return locations;
    }
}
