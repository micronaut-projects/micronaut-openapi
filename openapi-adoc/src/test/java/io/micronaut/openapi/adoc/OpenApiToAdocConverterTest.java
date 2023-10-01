package io.micronaut.openapi.adoc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import freemarker.template.TemplateException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiToAdocConverterTest {

    final Path outputDir = Paths.get("build/test/freemarker");

    @BeforeEach
    void setup() throws IOException {
        if (Files.exists(outputDir)) {
            Files.walk(outputDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    void testFreemarker() throws IOException, TemplateException {

        System.setProperty(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_OPENAPI_PATH, "/yaml/swagger_petstore.yaml");
        System.setProperty(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME, "myresult.adoc");
        System.setProperty(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH, outputDir.toString());
        System.setProperty(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATES_DIR_PATH, "classpath:/customDir");
        System.setProperty(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATE_PREFIX + "links", "links1.ftl");

        OpenApiToAdocConverter.convert();

        var resultFile = outputDir.resolve("myresult.adoc");
        assertTrue(Files.exists(resultFile));

        var adoc = Files.readString(resultFile);
        assertTrue(adoc.contains("!!!!!!test custom template"));
    }
}
