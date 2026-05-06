package com.info25.journalindex.util;

import java.util.List;

import lombok.Data;

@Data
public class PageableResponse<T> {
    int pages;
    List<T> data;
}
