package com.smartflow.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility for formatting and parsing dates cleanly.
 */
public class DateUtil {
    public static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String format(LocalDate date) {
        return date != null ? date.format(ISO_FORMAT) : "";
    }

    public static LocalDate parse(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), ISO_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
