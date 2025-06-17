package com.info25.journalindex.util;

import java.util.ArrayList;

public class SolrQueryAssembler {
    ArrayList<String> terms = new ArrayList<>();
    String combiningTerm = "AND";
    public SolrQueryAssembler() {}

    public SolrQueryAssembler addTerm(String term) {
        terms.add(term);
        return this;
    }

    public SolrQueryAssembler setCombiningTerm(String combiningTerm) {
        this.combiningTerm = combiningTerm;
        return this;
    }

    public String getFullQuery() {
        StringBuilder query = new StringBuilder();
        for (String term : terms) {
            if (query.length() > 0) {
                query.append(" " + combiningTerm + " ");
            }
            query.append("(" + term  + ")");
        }
        return query.toString();
    }
}
