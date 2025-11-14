package com.info25.journalindex.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.services.SolrClient;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.SolrSelectQuery;
import org.apache.coyote.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;

@RestController
@RequestMapping("/api/stats")
public class Stats {
    record MonthYear(int month, int year) {}

    @Autowired
    FileRepository fileRepository;

    @Autowired
    SolrClient solrClient;

    @PostMapping(value="/countEntries", produces={"application/json"})
    public String countEntries(@RequestParam("journals") int[] journals) {
        ObjectMapper om  = new ObjectMapper();
        ObjectNode x = om.createObjectNode();

        if (journals.length == 0) {
            journals = null;
        }

        x.set("totalEntries", new IntNode(fileRepository.totalEntries(journals)));
        x.set("totalDates", new IntNode(fileRepository.totalDates(journals)));

        return x.toString();
    }

    @PostMapping(value="/termFrequency", produces={"application/json"})
    public String termFrequency(@RequestParam("term") String term,
                                @RequestParam("journals") int[] journals) {
        if (term.contains(" ")) {
            term  = term.replaceAll(" ", "");
        }
        ObjectMapper om  = new ObjectMapper();
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
                .setRows(999999); // 32-bit int limit

        JsonNode results = solrClient.select(selectQuery);
        System.out.println(results.get("response").get("numFound"));
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
}
