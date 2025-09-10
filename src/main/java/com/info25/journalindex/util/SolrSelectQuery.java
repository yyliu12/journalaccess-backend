package com.info25.journalindex.util;

/**
 * A class to store properties that can be given in a Solr search
 */
public class SolrSelectQuery {
    String q;
    String fl;
    String sort;
    int rows;
    int start;
    String hl;
    String hlFl;
    String fq;


    public SolrSelectQuery(String q) {
        this.q = q;
        this.fl = "*";
        this.sort = "score desc";
        this.rows = 10;
        this.start = 0;
        this.hl = "false";
        this.hlFl = "";
        this.fq = "";
    }

    public SolrSelectQuery() {
        this.q = "*:*";
        this.fl = "*";
        this.sort = "score desc";
        this.rows = 10;
        this.start = 0;
        this.hl = "false";
        this.hlFl = "";
        this.fq = "";

    }



    public String getFq() {
        return fq;
    }

    public SolrSelectQuery setFq(String fq) {
        this.fq = fq;
        return this;
    }


    public String getHl() {
        return hl;
    }

    public SolrSelectQuery setHl(String hl) {
        this.hl = hl;
        return this;
    }

    public String getHlFl() {
        return hlFl;
    }

    public SolrSelectQuery setHlFl(String hlFl) {
        this.hlFl = hlFl;
        return this;
    }

    public SolrSelectQuery setFl(String fl) {
        this.fl = fl;
        return this;
    }

    public SolrSelectQuery setSort(String sort) {
        this.sort = sort;
        return this;
    }

    public SolrSelectQuery setRows(int rows) {
        this.rows = rows;
        return this;
    }

    public SolrSelectQuery setStart(int start) {
        this.start = start;
        return this;
    }

    public SolrSelectQuery setQ(String q) {
        this.q = q;
        return this;
    }

    public String getQ() {
        return q;
    }

    public String getFl() {
        return fl;
    }

    public String getSort() {
        return sort;
    }

    public int getRows() {
        return rows;
    }

    public int getStart() {
        return start;
    }
}
