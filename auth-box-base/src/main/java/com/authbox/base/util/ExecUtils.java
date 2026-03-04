package com.authbox.base.util;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;

/**
 * Utility class for executing external system commands.
 * <p>
 * This class leverages {@link ProcessBuilder} to run commands and captures their
 * standard output and error streams. Execution time is monitored and logged
 * using a {@link Stopwatch}.
 */
@Slf4j
public class ExecUtils {

    private static final Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws IllegalStateException if called.
     */
    private ExecUtils() {
        throw new IllegalStateException("Use static methods directly, without using constructor");
    }

    /**
     * Executes a system command and returns the combined standard output and error.
     * <p>
     * The command string is split into tokens by spaces. Error streams are redirected
     * to the input stream to ensure all output is captured in the returned string.
     *
     * @param command The full command line string to execute.
     * @return The resulting output from the command execution.
     * @throws IOException          If an I/O error occurs during process start or stream reading.
     * @throws InterruptedException If the current thread is interrupted while waiting for the process.
     */
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
