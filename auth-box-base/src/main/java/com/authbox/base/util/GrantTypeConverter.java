package com.authbox.base.util;

import com.authbox.base.model.GrantType;
import jakarta.persistence.AttributeConverter;

import java.util.List;
import java.util.stream.Collectors;

import static com.authbox.base.config.Constants.COMMA;
import static com.authbox.base.config.Constants.CSV_SPLITTER;

/**
 * JPA Attribute Converter to persist a list of {@link GrantType} enums as a comma-separated string.
 * <p>
 * This converter allows a collection of grant types to be stored in a single database column
 * by serializing the list into a CSV format and deserializing it back into a {@link List}.
 */
public class GrantTypeConverter implements AttributeConverter<List<GrantType>, String> {

    /**
     * Converts a list of {@link GrantType} constants into a single comma-separated string.
     *
     * @param grantTypes The list of grant types to be converted.
     * @return A string containing the enum names joined by {@link com.authbox.base.config.Constants#COMMA}.
     */
    @Override
    public String convertToDatabaseColumn(final List<GrantType> grantTypes) {
        return grantTypes.stream().map(Enum::name).collect(Collectors.joining(COMMA));
    }

    /**
     * Converts a comma-separated string from the database back into a list of {@link GrantType} enums.
     * <p>
     * The input string is split using {@link com.authbox.base.config.Constants#CSV_SPLITTER}.
     *
     * @param s The CSV string from the database column.
     * @return A list of {@link GrantType} constants reconstructed from the string.
     * @throws IllegalArgumentException if any string fragment does not match a valid {@link GrantType}.
     */
    @Override
    public List<GrantType> convertToEntityAttribute(final String s) {
        return CSV_SPLITTER.splitToList(s).stream().map(GrantType::valueOf).toList();
    }
}
