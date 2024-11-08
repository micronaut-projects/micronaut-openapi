package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import spock.util.environment.RestoreSystemProperties

class OpenApiSchemaWithMicronautStarterSpec extends AbstractOpenApiTypeElementSpec {

    @RestoreSystemProperties
    void "test conversion schemas with additionalProperties without exception"() {

        given:
        System.setProperty("api.version", "4.7.0")

        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.micronaut.core.annotation.Creator;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.naming.Named;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpParameters;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

@OpenAPIDefinition(
        info = @Info(
                title = "Micronaut Launch",
                version = "${api.version}",
                description = "API for Creating Micronaut Applications",
                license = @License(name = "Apache 2.0")
        )
)
@Controller
class ControllerExternal {

    /**
     * Information about this instance.
     * @return Information about this instance.
     */
    @Get("/versions")
    VersionDTO getInfo(@Parameter(hidden = true) RequestInfo info) {
        return new VersionDTO()
                .addLink(Relationship.SELF, info.self());
    }
}

/**
 * Information about the application.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@Introspected
@Schema(name = "Version")
class VersionDTO extends Linkable {

    /**
     * @return The version
     */
    public Map<String, String> getVersions() {
        return VersionInfo.getDependencyVersions();
    }

    @Override
    public VersionDTO addLink(CharSequence rel, LinkDTO link) {
        super.addLink(rel, link);
        return this;
    }
}

/**
 * A linkable type.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@Introspected
abstract class Linkable {
    private final Map<String, LinkDTO> links = new LinkedHashMap<>();

    /**
     * @return The links
     */
    @Schema(description = "Links to other resources")
    @JsonProperty("_links")
    @ReflectiveAccess
    public Map<String, LinkDTO> getLinks() {
        return links;
    }

    /**
     * Adds a link.
     * @param rel The relationship
     * @param link The link
     * @return this link
     */
    public Linkable addLink(CharSequence rel, LinkDTO link) {
        if (link != null && rel != null) {
            links.put(rel.toString(), link);
        }
        return this;
    }
}

/**
 * Represents a link.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@Introspected
@Schema(name = "Link")
class LinkDTO {

    private final String href;
    private final boolean templated;

    public LinkDTO(String href) {
        this(href, false);
    }

    @Creator
    public LinkDTO(String href, boolean templated) {
        this.href = href;
        this.templated = templated;
    }

    /**
     * @return The link address
     */
    @Schema(description = "The link address")
    public String getHref() {
        return href;
    }

    /**
     * @return Whether the link is templated
     */
    @Schema(description = "Whether the link is templated")
    public boolean isTemplated() {
        return templated;
    }
}

/**
 * Relationship types.
 *
 * @author graemerocher
 * @since 1.0.0
 */
enum Relationship implements Named, CharSequence {
    SELF,
    CREATE,
    PREVIEW,
    DIFF;

    @NonNull
    @Override
    public String getName() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public int length() {
        return getName().length();
    }

    @Override
    public char charAt(int index) {
        return getName().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return getName().subSequence(start, end);
    }

    @Override
    public String toString() {
        return getName();
    }
}

enum ApplicationType implements Named {

    DEFAULT("Micronaut Application", "A Micronaut Application"),
    CLI("Micronaut CLI Application", "A Command Line Application"),
    FUNCTION("Micronaut Serverless Function", "A Function Application for Serverless"),
    GRPC("Micronaut gRPC Application", "A gRPC Application"),
    MESSAGING("Micronaut Messaging Application", "A Messaging-Driven Application");

    public static final ApplicationType DEFAULT_OPTION = DEFAULT;

    private final String title;
    private final String description;

    ApplicationType(String title,
                    String description) {
        this.title = title;
        this.description = description;
    }

    /**
     * @return The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return The description.
     */
    public String getDescription() {
        return description;
    }

    @NonNull
    @Override
    public String getName() {
        return name().toLowerCase(Locale.ENGLISH);
    }
}

class VersionInfo {

    private static final Properties VERSIONS = new Properties();

    static {
        URL resource = VersionInfo.class.getResource("/micronaut-versions.properties");
        if (resource != null) {
            try (Reader reader = new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)) {
                VERSIONS.load(reader);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * @return The starter version
     */
    public static String getStarterVersion() {
        Package aPackage = VersionInfo.class.getPackage();
        if (aPackage != null) {
            String implementationVersion = aPackage.getImplementationVersion();
            if (implementationVersion != null) {
                return implementationVersion;
            }
        }
        return getMicronautVersion();
    }

    /**
     * @return Checks whether the starter is a snapshot version.
     */
    public static boolean isStarterSnapshot() {
        return getStarterVersion().endsWith("-SNAPSHOT");
    }

    /**
     * @return Checks whether micronaut is a snapshot version.
     */
    public static boolean isMicronautSnapshot() {
        return getMicronautVersion().endsWith("-SNAPSHOT");
    }

    public static String getMicronautVersion() {
        Object micronautVersion = VERSIONS.get("micronaut.version");
        if (micronautVersion != null) {
            return micronautVersion.toString();
        }
        return "4.0.0";
    }

    public static Optional<Integer> getMicronautMajorVersion() {
        return getMajorVersion(getMicronautVersion());
    }

    public static Optional<Integer> getMajorVersion(String version) {
        String[] parts = version.split("\\\\.");
        if (parts.length >= 1) {
            try {
                return Optional.of(Integer.parseInt(parts[0]));
            } catch (NumberFormatException e) {
                //
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the dependency versions.
     *
     * @return The versions
     */
    public static Map<String, String> getDependencyVersions() {
        Map<String, String> map = new LinkedHashMap<>();
        VERSIONS.entrySet().stream().sorted(Comparator.comparing(o -> o.getKey().toString()))
                .forEach(entry -> map.put(entry.getKey().toString(), entry.getValue().toString()));
        return Collections.unmodifiableMap(map);
    }

    /**
     * Get a dependency version from the BOM for the given ID.
     * @param id The ID
     * @return The dependency version as a string
     */
    public static @NonNull String getBomVersion(String id) {
        String key = id + ".version";
        Object version = VERSIONS.get(key);
        if (version != null) {
            return version.toString();
        }
        throw new IllegalArgumentException("Could not get version for ID " + id);
    }

    /**
     * Get a dependency version for the given ID.
     * @param id The ID
     * @return The dependency version
     */
    public static @NonNull Map.Entry<String, String> getDependencyVersion(String id) {
        String key = id + ".version";
        Object version = VERSIONS.get(key);
        if (version != null) {
            return new Map.Entry<String, String>() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public String getValue() {
                    return version.toString();
                }

                @Override
                public String setValue(String value) {
                    throw new UnsupportedOperationException("Cannot set version");
                }
            };
        }
        throw new IllegalArgumentException("Could not get version for ID " + id);
    }

    public static JdkVersion getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        // Allow these formats:
        // 1.8.0_72-ea
        // 9-ea
        // 9
        // 9.0.1
        int dotPos = version.indexOf('.');
        int dashPos = version.indexOf('-');
        return JdkVersion.valueOf(Integer.parseInt(version.substring(0,
                dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : version.length())));
    }

    public static String toJdkVersion(int javaVersion) {
        String jdkVersion = String.valueOf(javaVersion);
        return javaVersion <= 8 ? "1." + jdkVersion : jdkVersion;
    }

}

/**
 * JDK versions.
 * <a href="https://www.java.com/releases/">Releases</a>
 * @author graemerocher
 * @since 1.0.0
 */
@Serdeable
final class JdkVersion {

    private static final Map<Integer, JdkVersion> INSTANCES = new TreeMap<>();

    public static final JdkVersion JDK_8 = new JdkVersion(8);
    public static final JdkVersion JDK_9 = new JdkVersion(9);
    public static final JdkVersion JDK_10 = new JdkVersion(10);
    public static final JdkVersion JDK_11 = new JdkVersion(11);
    public static final JdkVersion JDK_12 = new JdkVersion(12);
    public static final JdkVersion JDK_13 = new JdkVersion(13);
    public static final JdkVersion JDK_14 = new JdkVersion(14);
    public static final JdkVersion JDK_15 = new JdkVersion(15);
    public static final JdkVersion JDK_16 = new JdkVersion(16);
    public static final JdkVersion JDK_17 = new JdkVersion(17);
    public static final JdkVersion JDK_18 = new JdkVersion(18);
    public static final JdkVersion JDK_19 = new JdkVersion(19);
    public static final JdkVersion JDK_20 = new JdkVersion(20);
    public static final JdkVersion JDK_21 = new JdkVersion(21);
    private static final String PREFIX_JDK = "JDK_";

    int majorVersion;

    public JdkVersion(int majorVersion) {
        this.majorVersion = majorVersion;
        INSTANCES.put(majorVersion, this);
    }

    /**
     * @return the name
     */
    @JsonValue
    public String name() {
        return PREFIX_JDK + majorVersion;
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JdkVersion other && other.majorVersion == majorVersion;
    }

    @Override
    public int hashCode() {
        return majorVersion;
    }

    @JsonCreator
    public static JdkVersion valueOf(String jdkVersion) {
        if (StringUtils.isEmpty(jdkVersion)) {
            throw new IllegalArgumentException("cannot parse JdkVersion from " + jdkVersion);
        }
        if (!jdkVersion.startsWith(PREFIX_JDK)) {
            throw new IllegalArgumentException("cannot parse JdkVersion from " + jdkVersion);
        }
        String version = jdkVersion.substring(PREFIX_JDK.length());
        try {
            int majorVersion = Integer.parseInt(version);
            return valueOf(majorVersion);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("cannot parse JDK major version of " + version);
        }
    }

    public static JdkVersion valueOf(int majorVersion) {
        return INSTANCES.values().stream()
                .filter(jdkVersion -> jdkVersion.majorVersion == majorVersion)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported JDK version: " + majorVersion + ". Supported values are " + INSTANCES.keySet()));
    }

    public int majorVersion() {
        return majorVersion;
    }

    // for serialization
    int getMajorVersion() {
        return majorVersion;
    }

    public boolean greaterThanEqual(@NonNull JdkVersion jdk) {
        return majorVersion >= jdk.majorVersion;
    }

    public String asString() {
        return "" + majorVersion;
    }
}


/**
 * The server URL.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@Hidden
class RequestInfo {

    public static final RequestInfo LOCAL = new RequestInfo("http://localhost:8080", "/", null, Locale.ENGLISH, "");

    private final String serverURL;
    private final String currentURL;
    private final String path;
    private final HttpParameters parameters;
    private final Locale locale;
    private final String userAgent;

    /**
     * Default constructor.
     * @param serverURL The URL
     * @param locale The locale
     */
    public RequestInfo(String serverURL, String path, HttpParameters parameters, Locale locale, String userAgent) {
        this.serverURL = Objects.requireNonNull(serverURL, "URL cannot be null");
        this.locale = locale;
        this.path = path;
        this.parameters = parameters;
        this.userAgent = userAgent;
        this.currentURL = serverURL + Objects.requireNonNull(path, "Path cannot be null");
    }

    /**
     * @return The server URL
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * @return The current URL
     */
    public String getCurrentURL() {
        return currentURL;
    }

    /**
     * @return The self link
     */
    public LinkDTO self() {
        return new LinkDTO(getCurrentURL(), false);
    }

    /**
     * @param rel The relationship
     * @param type The type
     * @return A new link
     */
    public LinkDTO link(Relationship rel, ApplicationType type) {
        return new LinkDTO(getServerURL() + "/" + rel + "/" + type.getName() + "/{name}");
    }

    /**
     * @param type The type
     * @return A new link
     */
    public LinkDTO link(ApplicationType type) {
        return new LinkDTO(getServerURL() + "/application-types/" + type.getName(), false);
    }

    public Locale getLocale() {
        return this.locale;
    }

    public LinkDTO link(String uri) {
        return new LinkDTO(getServerURL() + uri, false);
    }

    public String getUserAgent() {
        return userAgent;
    }

    /**
     * @return request path
     */
    public String getPath() {
        return path;
    }

    /**
     * @return request parameters
     */
    public HttpParameters getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return """
                RequestInfo{\\
                serverURL='%s', \\
                currentURL='%s', \\
                path='%s', \\
                parameters=%s, \\
                locale=%s, \\
                userAgent='%s'\\
                }""".formatted(serverURL, currentURL, path, parameters, locale, userAgent);
    }
}


@Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference
        def paths = openAPI.paths

        then:
        paths
        paths."/versions"
    }
}
