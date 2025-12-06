package com.info25.journalindex.controllers;

import com.external.PorterStemmer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.services.SolrClient;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.SolrSelectQuery;

import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

@RestController
@RequestMapping("/api/stats")
public class Stats {
    record MonthYear(int month, int year) {
    }

    @Autowired
    FileRepository fileRepository;

    @Autowired
    SolrClient solrClient;

    @PostMapping(value = "/countEntries", produces = { "application/json" })
    public String countEntries(@RequestParam("journals") int[] journals) {
        ObjectMapper om = new ObjectMapper();
        ObjectNode x = om.createObjectNode();

        if (journals.length == 0) {
            journals = null;
        }

        x.set("totalEntries", new IntNode(fileRepository.totalEntries(journals)));
        x.set("totalDates", new IntNode(fileRepository.totalDates(journals)));

        return x.toString();
    }

    @PostMapping(value = "/termFrequency", produces = { "application/json" })
    public String termFrequency(@RequestParam("term") String term,
            @RequestParam("journals") int[] journals) {
        if (term.contains(" ")) {
            term = term.replaceAll(" ", "");
        }
        ObjectMapper om = new ObjectMapper();
        ObjectNode x = om.createObjectNode();

        HashMap<MonthYear, Integer> data = new HashMap<>();
        String termFreqFunction = "termfreq(content,\"" + term + "\")";

        String q = "content:(" + term + ")";

        if (journals.length > 0)
            q = q + " AND journal_id:(" +
                    String.join(" OR ", Arrays.stream(journals).mapToObj(String::valueOf).toList()) +
                    ")";

        SolrSelectQuery selectQuery = new SolrSelectQuery()
                .setQ(q)
                .setFl("date," + termFreqFunction)
                .setSort("date desc")
                .setRows(999999);

        JsonNode results = solrClient.select(selectQuery);
        JsonNode docs = results.get("response").get("docs");

        for (int i = 0; i < docs.size(); i++) {
            JsonNode doc = docs.get(i);

            LocalDate date = DateUtils.timestampToLocalDate(doc.get("date").asLong());

            int freq = doc.get(termFreqFunction).asInt();

            if (!data.containsKey(new MonthYear(date.getMonthValue(), date.getYear()))) {
                data.put(new MonthYear(date.getMonthValue(), date.getYear()), freq);
            } else {
                data.put(new MonthYear(date.getMonthValue(), date.getYear()),
                        data.get(new MonthYear(date.getMonthValue(), date.getYear())) + freq);
            }

        }

        for (MonthYear monthYear : data.keySet()) {
            x.set(monthYear.month + "-" + monthYear.year, new IntNode(data.get(monthYear)));
        }

        return x.toString();
    }

    @PostMapping(value = "/termFrequencyMultiple", produces = { "application/json" })
    public String termFrequencyMultiple(@RequestParam("terms") JsonNode terms,
            @RequestParam("journals") int[] journals) {
        ObjectMapper om = new ObjectMapper();
        ObjectNode x = om.createObjectNode();

        for (JsonNode searchTerm : terms) {
            String searchString = searchTerm.asText();
            String[] searchStringTerms = Arrays.stream(searchString.split(","))
                    .map(String::trim).toArray(String[]::new);
            
            ArrayList<String> queryTerm = new ArrayList<>();
            ArrayList<String> termFreqFunctions = new ArrayList<>();
            ArrayList<String> exactTerms = new ArrayList<>();

            for (String s : searchStringTerms) {
                if (s.contains(" ")) {
                    exactTerms.add(s);
                    queryTerm.add("\"" + s + "\"");
                } else {
                    queryTerm.add(s);
                    termFreqFunctions.add("termfreq(content,\"" + s + "\")");
                }
            }

            String q = "content:(" + String.join(" OR ", queryTerm) + ")";

            if (journals.length > 0)
                q = q + " AND journal_id:(" +
                    String.join(" OR ", Arrays.stream(journals).mapToObj(String::valueOf).toList()) +
                    ")";


            SolrSelectQuery selectQuery = new SolrSelectQuery()
                    .setQ(q)
                    .setFl(termFreqFunctions.isEmpty() ? "date,content" : "date,content," + String.join(",", termFreqFunctions))
                    .setSort("date desc")
                    .setRows(999999);

            JsonNode results = solrClient.select(selectQuery);

            HashMap<String, Integer> data = new HashMap<>();

            JsonNode docs = results.get("response").get("docs");

            for (JsonNode doc : docs) {
                LocalDate date = DateUtils.timestampToLocalDate(doc.get("date").asLong());

                int freq = 0;
                for (String func : termFreqFunctions) {
                    freq += doc.get(func).asInt();
                }
                String content = doc.get("content").asText().toLowerCase();
                for (String exactTerm : exactTerms) {
                    String stemmedExactTerm = cleanString(exactTerm);
                    freq += StringUtils.countMatches(cleanString(content), stemmedExactTerm);
                }

                String key = date.getMonthValue() + "-" + date.getYear();

                if (!data.containsKey(key)) {
                    data.put(key, freq);
                } else {
                    data.put(key, data.get(key) + freq);
                }
            }

            x.set(searchString, om.valueToTree(data));

        }

        return x.toString();

    }

    public String cleanString(String s) {
        String output = s;
        output = output.toLowerCase();

        output = output.replace("“", "\""); // Left double quote
        output = output.replace("”", "\""); // Right double quote
        output = output.replace("‟", "\""); // Double prime (another variant)

        // Replace directional single quotes/apostrophes with plain single quotes
        output = output.replace("‘", "'"); // Left single quote
        output = output.replace("’", "'"); // Right single quote
        output = output.replace("‛", "'"); // Single prime (another variant)
        output = output.replace("’", "'"); // Apostrophe (common smart apostrophe)
    
        PorterStemmer ps = new PorterStemmer();
        return ps.stemWords(output);
    }
}
