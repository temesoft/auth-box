package com.authbox.base.util;

import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecUtilsTest {

    @Test
    public void testExecUtils_Success() throws IOException, InterruptedException {
        val output = ExecUtils.executeCommand("echo testing");
        assertThat(output).isNotNull().contains("testing");
    }

    @Test
    public void testExecUtils_Failure() {
        assertThatThrownBy(() -> ExecUtils.executeCommand(RandomStringUtils.secure().nextAlphabetic(25)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("error");
    }
}