package io.micronaut.openapi.visitor;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.adoc.OpenApiToAdocConverter;
import io.micronaut.openapi.visitor.group.OpenApiInfo;

import static io.micronaut.openapi.visitor.FileUtils.createDirectories;
import static io.micronaut.openapi.visitor.FileUtils.getDefaultFilePath;
import static io.micronaut.openapi.visitor.FileUtils.resolve;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME;

public class AdocModule {

    public void convert(OpenApiInfo openApiInfo, Map<String, String> props, VisitorContext context) {

        try {
            var writer = new StringWriter();
            OpenApiToAdocConverter.convert(openApiInfo.getOpenApi(), props, writer);

            var adoc = writer.toString();

            var outputPath = getOutputPath(props, context);
            context.info("Writing AsciiDoc OpenAPI file to destination: " + outputPath);
            context.getClassesOutputPath().ifPresent(path -> {
                // add relative paths for the specPath, and its parent META-INF/swagger
                // so that micronaut-graal visitor knows about them
                context.addGeneratedResource(path.relativize(outputPath).toString());
            });

            if (Files.exists(outputPath)) {
                Files.writeString(outputPath, adoc, StandardOpenOption.APPEND);
            } else {
                Files.writeString(outputPath, adoc);
            }
        } catch (Exception e) {
            context.warn("Can't convert to ADoc format\n" + Utils.printStackTrace(e), null);
        }
    }

    private Path getOutputPath(Map<String, String> props, VisitorContext context) {

        var fileName = props.getOrDefault(MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME, "openApiDoc.adoc");

        Path outputPath;
        String outputDir = props.get(MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH);
        if (StringUtils.isNotEmpty(outputDir)) {
            outputPath = resolve(context, Paths.get(outputDir));
        } else {
            outputPath = getDefaultFilePath(fileName, context).get().getParent();
        }
        outputPath = outputPath.resolve(fileName);
        createDirectories(outputPath, context);

        return outputPath;
    }
}
