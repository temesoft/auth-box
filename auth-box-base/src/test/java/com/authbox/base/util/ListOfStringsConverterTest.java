package com.authbox.base.util;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListOfStringsConverterTest {

    private final ListOfStringsConverter converter = new ListOfStringsConverter();

    @Test
    void testConvertToDatabaseColumnWithMultipleStrings() {
        val input = List.of("alpha", "beta", "gamma");
        val result = converter.convertToDatabaseColumn(input);
        assertThat(result).isEqualTo("alpha,beta,gamma");
    }

    @Test
    void testConvertToDatabaseColumnWithSingleString() {
        val input = List.of("delta");
        val result = converter.convertToDatabaseColumn(input);
        assertThat(result).isEqualTo("delta");
    }

    @Test
    void testConvertToEntityAttributeWithCsv() {
        val input = "one,two,three";
        val result = converter.convertToEntityAttribute(input);
        assertThat(result).containsExactly("one", "two", "three");
    }

    @Test
    void testConvertToEntityAttributeWithSingleValue() {
        val input = "four";
        val result = converter.convertToEntityAttribute(input);
        assertThat(result).containsExactly("four");
    }
}