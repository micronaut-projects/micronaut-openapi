/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.visitor.management;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Endpoint utilities.
 *
 * @since 6.16.0
 */
@Internal
public final class EndpointUtils {

    public static final Map<String, String> ALL_MICRONAUT_MANAGEMENT_ENDPOINTS = Map.of(
        "beans", "io.micronaut.management.endpoint.beans.BeansEndpoint",
        "env", "io.micronaut.management.endpoint.env.EnvironmentEndpoint",
        "health", "io.micronaut.management.endpoint.health.HealthEndpoint",
        "info", "io.micronaut.management.endpoint.info.InfoEndpoint",
        "loggers", "io.micronaut.management.endpoint.loggers.LoggersEndpoint",
        "refresh", "io.micronaut.management.endpoint.refresh.RefreshEndpoint",
        "routes", "io.micronaut.management.endpoint.routes.RoutesEndpoint",
        "stop", "io.micronaut.management.endpoint.stop.ServerStopEndpoint",
        "threaddump", "io.micronaut.management.endpoint.threads.ThreadDumpEndpoint"
    );

    public static final Map<String, String> ALL_SPRING_ACTUATOR_ENDPOINTS = Map.ofEntries(
        entry("auditevents", "org.springframework.boot.actuate.audit.AuditEventsEndpoint"),
        entry("beans", "org.springframework.boot.actuate.beans.BeansEndpoint"),
        entry("caches", "org.springframework.boot.actuate.cache.CachesEndpoint"),
        entry("shutdown", "org.springframework.boot.actuate.context.ShutdownEndpoint"),
        entry("env", "org.springframework.boot.actuate.env.EnvironmentEndpoint"),
        entry("flyway", "org.springframework.boot.actuate.flyway.FlywayEndpoint"),
        entry("health", "org.springframework.boot.actuate.health.HealthEndpoint"),
        entry("info", "org.springframework.boot.actuate.info.InfoEndpoint"),
        entry("integrationgraph", "org.springframework.boot.actuate.integration.IntegrationGraphEndpoint"),
        entry("liquibase", "org.springframework.boot.actuate.liquibase.LiquibaseEndpoint"),
        entry("logfile", "org.springframework.boot.actuate.logging.LogFileWebEndpoint"),
        entry("loggers", "org.springframework.boot.actuate.logging.LoggersEndpoint"),
        entry("heapdump", "org.springframework.boot.actuate.management.HeapDumpWebEndpoint"),
        entry("threaddump", "org.springframework.boot.actuate.management.ThreadDumpEndpoint"),
        entry("metrics", "org.springframework.boot.actuate.metrics.MetricsEndpoint"),
        entry("quartz", "org.springframework.boot.actuate.quartz.QuartzEndpoint"),
        entry("sbom", "org.springframework.boot.actuate.sbom.SbomEndpoint"),
        entry("scheduledtasks", "org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint"),
        entry("sessions", "org.springframework.boot.actuate.session.SessionsEndpoint"),
        entry("startup", "org.springframework.boot.actuate.startup.StartupEndpoint"),
        entry("httpexchanges", "org.springframework.boot.actuate.web.exchanges.HttpExchangesEndpoint"),
        entry("mappings", "org.springframework.boot.actuate.web.mappings.MappingsEndpoint"),
        entry("prometheus", "org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint")
    );

    /**
     * Endpoints, which can be processed only if specific classes in classpath.
     */
    public static final Map<String, String> SPECIFIC_ENDPOINTS = Map.of(
        "org.springframework.boot.actuate.integration.IntegrationGraphEndpoint", "org.springframework.integration.graph.IntegrationGraphServer",
        "org.springframework.boot.actuate.flyway.FlywayEndpoint", "org.flywaydb.core.Flyway",
        "org.springframework.boot.actuate.session.SessionsEndpoint", "org.springframework.session.Session",
        "org.springframework.boot.actuate.quartz.QuartzEndpoint", "org.quartz.Job",
        "org.springframework.boot.actuate.liquibase.LiquibaseEndpoint", "liquibase.integration.spring.SpringLiquibase"
    );

    public static final List<String> ALL_MANAGEMENT_ENDPOINT_CLASSES;

    static {
        var knownEndpoints = new ArrayList<>(ALL_MICRONAUT_MANAGEMENT_ENDPOINTS.values());
        knownEndpoints.addAll(ALL_SPRING_ACTUATOR_ENDPOINTS.values());
        ALL_MANAGEMENT_ENDPOINT_CLASSES = List.copyOf(knownEndpoints);
    }
    public static final String[] ALL_MANAGEMENT_ENDPOINT_CLASSES_ARRAY = ALL_MANAGEMENT_ENDPOINT_CLASSES.toArray(StringUtils.EMPTY_STRING_ARRAY);

    private EndpointUtils() {
    }
}
