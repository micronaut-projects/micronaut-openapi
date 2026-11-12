package io.micronaut.openapi.visitor;

import io.micronaut.context.env.Environment;

public final class OpenApiEnvironmentLoggingProbe {
    public static void run() throws Exception {
        try (Environment environment = ConfigUtils.getEnv(null)) {
            if (environment == null || !environment.isRunning()) {
                throw new AssertionError("OpenAPI environment did not start");
            }
            String path = environment.getRequiredProperty("environment.logging.probe", String.class);
            if (!"loaded".equals(path)) {
                throw new AssertionError("OpenAPI environment failed to load application.properties");
            }
        }
    }
}
