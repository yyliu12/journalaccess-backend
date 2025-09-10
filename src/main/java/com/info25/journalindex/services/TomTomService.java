package com.info25.journalindex.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.info25.journalindex.models.File.Location;

/**
 * Service for interacting with TomTom APIs
 */
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

    /**
     * Creates list of location objects from a search query submitted to the TomTom search API
     * @param query the search query
     * @return a list of Location objects
     */
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
        // lots of json parsing happening here
        try {
            JsonNode results = mapper.readTree(body).get("results");
            for (JsonNode result : results) {
                StringBuilder address = new StringBuilder();
                String buildingName = "";
                // poi usually specifies the buliding name (e.g. Brittany Hall)
                if (result.has("poi")) {
                    buildingName = result.get("poi").get("name").asText();
                }
                address.append(result.get("address").get("freeformAddress").asText());
                Location location = new Location(
                        // create lat, lon string in position field. yes, we store positions as strings because
                        // that's the way that solr stores them
                        result.get("position").get("lat").asText() + ", " + result.get("position").get("lon").asText(),
                        address.toString(),
                        buildingName
                );
                locations.add(location);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return locations;
    }
}
