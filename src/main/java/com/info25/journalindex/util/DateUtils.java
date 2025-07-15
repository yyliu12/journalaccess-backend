package com.info25.journalindex.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * This handles date conversions between timestamps (used in the db) and dates
 */
public class DateUtils {
    /**
     * DateTimeFormatter instance, formats using yyyy-MM-dd, which is the standard
     * used across the application
     */
    final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Defines the timezone that timestamps are created using
     * 
     * it just so happens that dates in the db were created using EST
     * it should be fine if we hardcode this as we only use dates in
     * the application; the timezone of the date doesn't really matter
     */
    final static ZoneId timezone = ZoneId.of("America/New_York");

    /**
     * Converts a timestamp (from db) to a LocalDate
     * @param timestamp the timestamp to convert from the db
     * @return a LocalDate representing the timestamp
     */
    public static LocalDate timestampToLocalDate(long timestamp) {
        return Instant.ofEpochSecond(timestamp)
                .atZone(timezone)
                .toLocalDate();
    }

    /**
     * Converts a LocalDate to a timestamp
     * @param date the LocalDate to convert
     * @return a long representing the timestamp to be used in the db
     */
    public static long localDateToTimestamp(LocalDate date) {
        return date.atStartOfDay(timezone).toEpochSecond();
    }

    /**
     * Parses a date string to a LocalDate
     * @param dateString the date string to parse
     * @return a LocalDate parsed from the string
     */
    public static LocalDate parseFromString(String dateString) {
        return LocalDate.parse(dateString, dateTimeFormatter);
    }

    /**
     * Formats a LocalDate to a string
     * @param date the LocalDate to format
     * @return a string representation of the LocalDate
     */
    public static String formatToString(LocalDate date) {
        return date.format(dateTimeFormatter);
    }

    
}
