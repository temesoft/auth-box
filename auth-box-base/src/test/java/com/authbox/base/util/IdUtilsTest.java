package com.authbox.base.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IdUtilsTest {

    @Test
    public void testVerifyCorrectIdGenerator() {
        assertThat(IdUtils.createId())
                .isNotBlank()
                .hasSize(27)
                .isNotEqualTo(IdUtils.createId());
    }
}