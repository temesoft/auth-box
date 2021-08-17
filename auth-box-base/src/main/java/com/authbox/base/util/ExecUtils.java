package com.authbox.base.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

public class ExecUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecUtils.class);
    private static final Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();
    private static final Joiner SPACE_JOINER = Joiner.on(" ").skipNulls();

    public static String executeScript(final String script) throws IOException, InterruptedException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final File batchFile = Files.createTempFile("java-exec-script", ".sh").toFile();
        final String completeScript = "#!/usr/bin/env bash\n\n" + script.replaceAll(" && ", "\n");
        try (final FileOutputStream out = new FileOutputStream(batchFile)) {
            try (final PrintWriter printWriter = new PrintWriter(out)) {
                printWriter.write(completeScript);
            }
        }
        Files.setPosixFilePermissions(Paths.get(batchFile.getAbsolutePath()), PosixFilePermissions.fromString("rwxrwxr-x"));
        LOGGER.debug("Executing shell script: {}", batchFile.getAbsolutePath());
        LOGGER.trace("\n{}", completeScript);
        final ProcessBuilder pb = new ProcessBuilder(ImmutableList.of(batchFile.getAbsolutePath()));
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        final String std;
        try (final InputStream stdIn = process.getInputStream()) {
            std = IOUtils.toString(stdIn);
            process.waitFor();
        }
        LOGGER.debug("Finished executing script in {}", stopwatch.stop());
        Files.deleteIfExists(batchFile.toPath());
        return std;
    }

    public static String executeCommand(final String command) throws IOException, InterruptedException {
        return executeCommand(SPACE_SPLITTER.splitToList(command));
    }

    public static String executeCommand(final List<String> commands) throws IOException, InterruptedException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOGGER.debug("Executing command: {}", SPACE_JOINER.join(commands));
        final ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        final String std;
        try (final InputStream stdIn = process.getInputStream()) {
            std = IOUtils.toString(stdIn);
            process.waitFor();
        }
        LOGGER.trace("\n{}", std);
        LOGGER.debug("Finished executing command in {}", stopwatch.stop());
        return std;
    }

    public static String executePipedCommand(final String pipedCommand) throws IOException, InterruptedException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOGGER.debug("Executing piped command: {}", pipedCommand);
        final ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", pipedCommand);
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        final String std;
        try (final InputStream stdIn = process.getInputStream()) {
            std = IOUtils.toString(stdIn);
            process.waitFor();
        }
        LOGGER.debug("Finished executing piped command in {}", stopwatch.stop());
        return std;
    }
}
