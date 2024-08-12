package com.authbox.base.util;

import com.authbox.base.model.GrantType;

import jakarta.persistence.AttributeConverter;
import java.util.List;
import java.util.stream.Collectors;

import static com.authbox.base.config.Constants.COMMA;
import static com.authbox.base.config.Constants.CSV_SPLITTER;

public class GrantTypeConverter implements AttributeConverter<List<GrantType>, String> {

    @Override
    public String convertToDatabaseColumn(final List<GrantType> grantTypes) {
        return grantTypes.stream().map(Enum::name).collect(Collectors.joining(COMMA));
    }

    @Override
    public List<GrantType> convertToEntityAttribute(final String s) {
        return CSV_SPLITTER.splitToList(s).stream().map(GrantType::valueOf).toList();
    }
}
