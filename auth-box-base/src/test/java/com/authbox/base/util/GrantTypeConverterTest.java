package com.authbox.base.util;

import com.authbox.base.model.GrantType;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GrantTypeConverterTest {

    private GrantTypeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new GrantTypeConverter();
    }

    @Test
    void testConvertToDatabaseColumnWithMultipleValues() {
        val grantTypes = List.of(GrantType.authorization_code, GrantType.refresh_token);
        val result = converter.convertToDatabaseColumn(grantTypes);
        assertThat(result).isEqualTo("authorization_code,refresh_token");
    }

    @Test
    void testConvertToDatabaseColumnWithSingleValue() {
        val grantTypes = List.of(GrantType.client_credentials);
        val result = converter.convertToDatabaseColumn(grantTypes);
        assertThat(result).isEqualTo("client_credentials");
    }

    @Test
    void testConvertToEntityAttributeWithMultipleValues() {
        val csv = "authorization_code,refresh_token";
        val result = converter.convertToEntityAttribute(csv);
        assertThat(result).containsExactly(GrantType.authorization_code, GrantType.refresh_token);
    }

    @Test
    void testConvertToEntityAttributeWithSingleValue() {
        val csv = "client_credentials";
        val result = converter.convertToEntityAttribute(csv);
        assertThat(result).containsExactly(GrantType.client_credentials);
    }
}