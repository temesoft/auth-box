package com.authbox.base.util;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.List;

@Slf4j
public class ExecUtils {

    private static final Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();

    private ExecUtils() {
    }

    public static String executeCommand(final String command) throws IOException, InterruptedException {
        val stopwatch = Stopwatch.createStarted();
        log.debug("Executing command: {}", command);
        val commands = SPACE_SPLITTER.splitToList(command);
        val pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);
        val process = pb.start();
        final String std;
        try (val stdIn = process.getInputStream()) {
            std = IOUtils.toString(stdIn);
            process.waitFor();
        }
        log.trace("\n{}", std);
        log.debug("Finished executing command in {}", stopwatch.stop());
        return std;
    }
}
