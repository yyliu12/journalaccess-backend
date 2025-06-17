package com.info25.journalindex.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LocalDate timestampToLocalDate(long timestamp) {
        return Instant.ofEpochSecond(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
    }

    public static long localDateToTimestamp(LocalDate date) {
        return date.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond();
    }

    public static LocalDate parseFromString(String dateString) {
        return LocalDate.parse(dateString, dateTimeFormatter);
    }

    public static String formatToString(LocalDate date) {
        return date.format(dateTimeFormatter);
    }

    
}
