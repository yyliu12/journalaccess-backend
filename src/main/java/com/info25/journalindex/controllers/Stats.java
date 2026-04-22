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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/stats")
public class Stats {
    record MonthYear(int month, int year) {
    }

    record DataReturn(Map<Integer, Map<Integer, Integer>> data1, Map<String, Integer> data2) {
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
    public DataReturn termFrequency(@RequestParam("term") String term,
            @RequestParam("journals") int[] journals) {
        if (term.contains(" ")) {
            term = term.replaceAll(" ", "");
        }

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

        Map<Integer, Map<Integer, Integer>> data1 = new HashMap<>();

        for (int i = 0; i < docs.size(); i++) {
            JsonNode doc = docs.get(i);

            LocalDate date = DateUtils.timestampToLocalDate(doc.get("date").asLong());

            int freq = doc.get(termFreqFunction).asInt();

            if (!data.containsKey(new MonthYear(date.getMonthValue(), date.getYear()))) {
                data.put(new MonthYear(date.getMonthValue(), date.getYear()), freq);

                data1.putIfAbsent(date.getYear(), new HashMap<>());
                data1.get(date.getYear()).put(date.getMonthValue(), freq);
            } else {
                data.put(new MonthYear(date.getMonthValue(), date.getYear()),
                        data.get(new MonthYear(date.getMonthValue(), date.getYear())) + freq);

                data1.get(date.getYear()).put(date.getMonthValue(),
                        data1.get(date.getYear()).get(date.getMonthValue()) + freq);
            }

        }

        Map<String, Integer> data2 = new HashMap<>();

        for (MonthYear monthYear : data.keySet()) {
            data2.put(monthYear.month + "-" + monthYear.year, data.get(monthYear));
        }

        return new DataReturn(data1, data2);
    }

    @PostMapping(value = "/termFrequencyMultiple", produces = { "application/json" })
    public Map<String, DataReturn> termFrequencyMultiple(@RequestParam("terms") JsonNode terms,
            @RequestParam("journals") int[] journals) {

        Map<String, DataReturn> x = new HashMap<>();

        for (JsonNode searchTerm : terms) {
            String searchString = searchTerm.asText();
            String[] searchStringTerms = Arrays.stream(searchString.split(","))
                    .map(String::trim).toArray(String[]::new);

            ArrayList<String> queryTerm = new ArrayList<>();
            ArrayList<String> exactTerms = new ArrayList<>();

            for (String s : searchStringTerms) {
                exactTerms.add(s);
                queryTerm.add("\"" + s + "\"");

            }

            String q = "content:(" + String.join(" OR ", queryTerm) + ")";

            if (journals.length > 0)
                q = q + " AND journal_id:(" +
                        String.join(" OR ", Arrays.stream(journals).mapToObj(String::valueOf).toList()) + ")";

            SolrSelectQuery selectQuery = new SolrSelectQuery()
                    .setQ(q)
                    .setFl("id,date,content")
                    .setSort("date desc")
                    .setRows(999999);

            JsonNode results = solrClient.select(selectQuery);

            HashMap<String, Integer> data2 = new HashMap<>();
            HashMap<Integer, Map<Integer, Integer>> data1 = new HashMap<>();

            JsonNode docs = results.get("response").get("docs");

            for (JsonNode doc : docs) {
                LocalDate date = DateUtils.timestampToLocalDate(doc.get("date").asLong());

                int freq = 0;

                String content = doc.get("content").asText().toLowerCase();
                String[] split = cleanString(content).split("[^a-zA-Z0-9]+");
                Set<Integer> matches = new HashSet<>();

                for (String exactTerm : exactTerms) {
                    int occurrences = 0;

                    String stemmedExactTerm = cleanString(exactTerm);
                    System.out.println(content);
                    String[] termSplit = stemmedExactTerm.split("[^a-zA-Z0-9]+");

                    for (int i = 0; i <= split.length - termSplit.length; i++) {
                        // attempt match
                        ArrayList<Integer> indicesMatched = new ArrayList<>();
                        int termMatchIndex = 0;


                        for (int j = i; j < i + termSplit.length; j++) {

                            if (matches.contains(j)) {
                                break;
                            }

                            if (split[j].equals(termSplit[termMatchIndex])) {
                                termMatchIndex++;
                                indicesMatched.add(j);

                                if (termMatchIndex == termSplit.length) {
                                    occurrences++;
                                    termMatchIndex = 0;
                                    matches.addAll(indicesMatched);
                                    indicesMatched.clear();
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                    String id = doc.get("id").asText();
                    System.out.println(id + " " + occurrences);
                    
                    freq += occurrences;
                }

                String key = date.getMonthValue() + "-" + date.getYear();

                if (!data2.containsKey(key)) {
                    data2.put(key, freq);
                    data1.putIfAbsent(date.getYear(), new HashMap<>());
                    data1.get(date.getYear()).put(date.getMonthValue(), freq);
                } else {
                    data2.put(key, data2.get(key) + freq);
                    data1.get(date.getYear()).put(date.getMonthValue(),
                            data1.get(date.getYear()).get(date.getMonthValue()) + freq);
                }
            }

            x.put(searchString, new DataReturn(data1, data2));

        }

        return x;

    }

    @PostMapping(value = "/freqStats", produces = { "application/json" })
    public DataReturn freqStats(@RequestParam("countFiles") boolean countFiles,
            @RequestParam("journals") int[] journals) {
        if (journals.length == 0) {
            journals = null;
        }
        Map<Integer, Map<Integer, Integer>> data1 = fileRepository.getFileCountsByYearAndMonth(countFiles, journals);
        Map<String, Integer> data2 = new HashMap<>();

        for (Integer year : data1.keySet()) {
            for (Integer month : data1.get(year).keySet()) {
                data2.put(month + "-" + year, data1.get(year).get(month));
            }
        }

        return new DataReturn(data1, data2);
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
