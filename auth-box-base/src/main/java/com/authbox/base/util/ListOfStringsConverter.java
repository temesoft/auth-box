package com.authbox.base.util;

import jakarta.persistence.AttributeConverter;

import java.util.List;

import static com.authbox.base.config.Constants.COMMA;
import static com.authbox.base.config.Constants.CSV_SPLITTER;

/**
 * JPA Attribute Converter to persist a list of strings as a comma-separated value in the database.
 * <p>
 * This converter facilitates storing simple string collections in a single database column
 * by joining elements with a comma during persistence and splitting them back into a
 * {@link List} during retrieval.
 */
public class ListOfStringsConverter implements AttributeConverter<List<String>, String> {

    /**
     * Converts a list of strings into a single string joined by {@link com.authbox.base.config.Constants#COMMA}.
     *
     * @param strings The list of string values to be converted.
     * @return A single comma-separated string, or {@code null} if the input list is null.
     */
    @Override
    public String convertToDatabaseColumn(final List<String> strings) {
        return String.join(COMMA, strings);
    }

    /**
     * Converts a comma-separated string from the database back into a list of strings.
     * <p>
     * Uses {@link com.authbox.base.config.Constants#CSV_SPLITTER} to perform the split operation.
     *
     * @param s The CSV string from the database column.
     * @return A {@link List} of strings, or an empty list if the input string is null or blank.
     */
    @Override
    public List<String> convertToEntityAttribute(final String s) {
        return CSV_SPLITTER.splitToList(s).stream().toList();
    }
}
