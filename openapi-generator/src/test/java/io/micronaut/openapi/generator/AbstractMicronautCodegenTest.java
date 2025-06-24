package io.micronaut.openapi.generator;

import com.tschuchort.compiletesting.KotlinCompilation;
import com.tschuchort.compiletesting.SourceFile;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
     * @param opts codegen options
     * @param configPath - the path to the config starting from src/test/resources
     * @param filesToGenerate - which files to generate - can be CodegenConstants. MODELS, APIS, SUPPORTING_FILES, ...
     *
     * @return - the path to the generated folder
     */
    protected String generateWithOpts(MicronautCodeGenerator<?> codegen, Map<String, Object> opts, String configPath, String... filesToGenerate) {
        if (opts == null) {
            throw new IllegalArgumentException("Codegen options are required");
        }
        File output = null;
        try {
            output = Files.createTempDirectory("test").toFile().getCanonicalFile();
        } catch (IOException e) {
            fail("Unable to create temporary directory for output");
        }
        output.deleteOnExit();

        MicronautCodeGeneratorEntryPoint.builder()
            .withOptions(optsBuilder -> {
                if (opts.containsKey("useUrlConnectionCache")) {
                    optsBuilder.withUseUrlConnectionCache((boolean) opts.get("useUrlConnectionCache"));
                }
                if (opts.containsKey("additionalProperties")) {
                    optsBuilder.withAdditionalProperties((Map<String, Object>) opts.get("additionalProperties"));
                }
            })
            .forCodeGenerator(codegen, unused -> {
            })
            .withDefinitionFile(new File(configPath).toURI())
            .withOutputDirectory(output)
            .withOutputs(Arrays.stream(filesToGenerate)
                .map(MicronautCodeGeneratorEntryPoint.OutputKind::of)
                .toArray(MicronautCodeGeneratorEntryPoint.OutputKind[]::new)
            )
            .build()
            .generate();

        // Create parser
        String outputPath = output.getAbsolutePath().replace('\\', '/');

        return outputPath + "/";
    }

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
        output.deleteOnExit();

        MicronautCodeGeneratorEntryPoint.builder()
            .forCodeGenerator(codegen, unused -> {
            })
            .withDefinitionFile(new File(configPath).toURI())
            .withOutputDirectory(output)
            .withOutputs(Arrays.stream(filesToGenerate)
                .map(MicronautCodeGeneratorEntryPoint.OutputKind::of)
                .toArray(MicronautCodeGeneratorEntryPoint.OutputKind[]::new)
            )
            .build()
            .generate();

        // Create parser
        String outputPath = output.getAbsolutePath().replace('\\', '/');

        return outputPath + "/";
    }

    /**
     * @see AbstractMicronautCodegenTest#assertFilesCompile(String, String, SourceFile...)
     */
    public static void assertFilesCompile(String directory, SourceFile... extraSourceFiles) {
        assertFilesCompile(directory, null, extraSourceFiles);
    }

    /**
     * Compile files using the kotlin compiler and assert the compilation succeeded
     *
     * @param directory        - path of a directory of generated files to be compiled
     * @param jvmTarget        - jvmTarget version to compile to
     * @param extraSourceFiles - extra source files to add to the compilation - useful for adding dummy types
     */
    public static void assertFilesCompile(String directory, String jvmTarget, SourceFile... extraSourceFiles) {
        String[] compilableFileExtensions = {".java", ".kt"};
        List<SourceFile> sourceFiles = new ArrayList<>();
        FileUtils.iterateFiles(new File(directory), compilableFileExtensions, true)
            .forEachRemaining(
                file -> sourceFiles.add(SourceFile.Companion.fromPath(file, false))
            );
        sourceFiles.addAll(List.of(extraSourceFiles));
        var compilation = new KotlinCompilation();
        compilation.setSources(sourceFiles);
        compilation.setInheritClassPath(true);
        if (jvmTarget != null) {
            compilation.setJvmTarget(jvmTarget);
        }
        var result = compilation.compile();
        assertEquals(KotlinCompilation.ExitCode.OK, result.getExitCode());
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
        Path path = Path.of(file);
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

    public static void assertFileDoesntExist(String path) {
        assertFalse(Path.of(path).toFile().exists(), "File \"" + path + "\" should not exist");
    }

    public static String readFile(String path) {
        String file = null;
        try {
            file = Files.readString(Path.of(path));
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
