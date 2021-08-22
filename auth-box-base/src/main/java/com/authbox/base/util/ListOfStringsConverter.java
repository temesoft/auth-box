package com.authbox.base.util;

import javax.persistence.AttributeConverter;
import java.util.List;
import java.util.stream.Collectors;

import static com.authbox.base.config.Constants.COMMA;
import static com.authbox.base.config.Constants.CSV_SPLITTER;

public class ListOfStringsConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(final List<String> strings) {
        return String.join(COMMA, strings);
    }

    @Override
    public List<String> convertToEntityAttribute(final String s) {
        return CSV_SPLITTER.splitToList(s).stream().collect(Collectors.toUnmodifiableList());
    }
}
