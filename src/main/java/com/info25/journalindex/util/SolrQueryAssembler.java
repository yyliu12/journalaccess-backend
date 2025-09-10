package com.info25.journalindex.util;

import java.util.ArrayList;

/**
 * A class, similar to a StringBuilder, that helps assemble solr search queries
 * 
 * Essentially, every search term added in the end will be combined with the combiner
 * when getFullQuery is called. (a combiner would be "AND" or "OR")
 */
public class SolrQueryAssembler {
    ArrayList<String> terms = new ArrayList<>();
    String combiningTerm = "AND";
    public SolrQueryAssembler() {}

    /**
     * Add a term to the search query
     * @param term
     * @return
     */
    public SolrQueryAssembler addTerm(String term) {
        terms.add(term);
        return this;
    }

    /**
     * Define what word to use to combine all the terms
     * @param combiningTerm
     * @return
     */
    public SolrQueryAssembler setCombiningTerm(String combiningTerm) {
        this.combiningTerm = combiningTerm;
        return this;
    }

    /**
     * Gets the full query. For example, if we had two terms:
     * 1) "content:Hello"
     * 2) "location:[-1,-1 TO 1,1]"
     * and the combiner was "AND", we would get "(content:Hello) AND (location:[-1,-1 TO 1,1])"
     * @return the full query
     */
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
