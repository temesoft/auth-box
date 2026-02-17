package com.authbox.base.model;

import io.github.temesoft.testpojo.TestPojo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

class PojoTest {
    @Test
    public void testAllModels() throws IOException {
        TestPojo.processPackage(AccessLog.class.getPackageName())
                .testAll()
                .saveReport(Path.of("./target/test-pojo-report.txt"));
    }
}
