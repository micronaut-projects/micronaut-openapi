package io.micronaut.openapi.test.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An abstract class with methods useful for testing
 */
public final class TestUtils {

    private TestUtils() {
    }

    public static void assertFileContainsRegex(String path, String... regex) {
        assertFileExists(path);
        String file = readFile(path);
        for (String line : regex) {
            assertTrue(Pattern.compile(line.replace(" ", "\\s+")).matcher(file).find());
        }
    }

    public static void assertFileNotContainsRegex(String path, String... regex) {
        assertFileExists(path);
        String file = readFile(path);
        for (String line : regex) {
            assertFalse(Pattern.compile(line.replace(" ", "\\s+")).matcher(file).find());
        }
    }

    public static void assertFileContains(String path, String... lines) {
        assertFileExists(path);
        String file = linearize(readFile(path));
        for (String line : lines) {
            assertTrue(file.contains(linearize(line)), "File does not contain line [" + line + "]");
        }
    }

    public static void assertFileNotContains(String path, String... lines) {
        assertFileExists(path);
        String file = linearize(readFile(path));
        for (String line : lines) {
            assertFalse(file.contains(linearize(line)), "File contains line [" + line + "]");
        }
    }

    public static void assertFileExists(String file) {
        Path path = Paths.get(file);
        if (!path.toFile().exists()) {
            while (path.getParent() != null && !path.getParent().toFile().exists()) {
                path = path.getParent();
            }
            String message = "File \"" + file + "\" should exist, however \"" + path + "\" could not be found.";
            if (path.getParent() != null) {
                Path parent = path.getParent();
                File[] contents = parent.toFile().listFiles();
                message += "\nContents of folder \"" + path.getParent() + "\": ";
                if (contents == null) {
                    message += null;
                } else {
                    message += Arrays.stream(contents)
                        .map(f -> f.toString().substring(parent.toString().length() + 1))
                        .toList();
                }
                message += ".";
            }
            fail(message);
        }
    }

    public static void assertFileNotExists(String path) {
        assertFalse(Paths.get(path).toFile().exists(), "File \"" + path + "\" should not exist");
    }

    public static String readFile(String path) {
        String file = null;
        try {
            file = Files.readString(Paths.get(path));
            assertNotNull(file, "File \"" + path + "\" does not exist");
        } catch (IOException e) {
            fail("Unable to evaluate file " + path);
        }

        return file;
    }

    public static String linearize(String target) {
        return target.replaceAll("\r?\n", "").replaceAll("\\s+", "s");
    }
}
