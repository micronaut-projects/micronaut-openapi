package io.micronaut.openapi.generator;

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
public abstract class AbstractMicronautCodegenTest {

    /**
     * Path to a common test configuration file
     */
    protected final String PETSTORE_PATH = "src/test/resources/petstore.json";

    /**
     * @param codegen - the code generator
     * @param configPath - the path to the config starting from src/test/resources
     * @param filesToGenerate - which files to generate - can be CodegenConstants. MODELS, APIS, SUPPORTING_FILES, ...
     *
     * @return - the path to the generated folder
     */
    protected String generateFiles(MicronautCodeGenerator<?> codegen, String configPath, String... filesToGenerate) {
        File output = null;
        try {
            output = Files.createTempDirectory("test").toFile().getCanonicalFile();
        } catch (IOException e) {
            fail("Unable to create temporary directory for output");
        }
//        output.deleteOnExit();

        MicronautCodeGeneratorEntryPoint.builder()
            .forCodeGenerator(codegen, unused -> {
            })
            .withDefinitionFile(new File(configPath).toURI())
            .withOutputDirectory(output)
            .withOutputs(Arrays.stream(filesToGenerate)
                .map(MicronautCodeGeneratorEntryPoint.OutputKind::of)
                .toList()
                .toArray(new MicronautCodeGeneratorEntryPoint.OutputKind[0])
            )
            .build()
            .generate();


        // Create parser
        String outputPath = output.getAbsolutePath().replace('\\', '/');

        return outputPath + "/";
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
