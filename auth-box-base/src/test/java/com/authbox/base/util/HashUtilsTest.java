package com.authbox.base.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HashUtilsTest {

    @Test
    public void testSha256() {
        assertThat(HashUtils.sha256("test"))
                .isEqualTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    }

    @Test
    public void makeRandomBase32() {
        assertThat(HashUtils.makeRandomBase32())
                .isNotBlank()
                .hasSize(64);
    }
}