package io.micronaut.configuration.openapi.docs.filters;

// tag::imports[]
import java.time.Duration;

import org.reactivestreams.Publisher;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
// end::imports[]

// tag::clazz[]
@Filter(
    methods = {HttpMethod.GET, HttpMethod.HEAD},
    patterns = {"/**/rapidoc*", "/**/redoc*", "/**/swagger-ui*", "/**/openapi-explorer*"}
)
@Requires(property = "micronaut.server.context-path-header")
public class OpenApiContextPathFilter implements HttpServerFilter {

    private final String contextPathHeader;

    OpenApiContextPathFilter(@Value("${micronaut.server.context-path-header}") String contextPathHeader) {
        this.contextPathHeader = contextPathHeader;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        final String contextPath = request.getHeaders().get(contextPathHeader);

        if (contextPath != null) {
            Cookie contextPathCookie = Cookie.of("contextPath", contextPath).maxAge(Duration.ofMinutes(2L));
            return Publishers.map(chain.proceed(request), response -> response.cookie(contextPathCookie));
        } else {
            return chain.proceed(request);
        }
    }

}
// end::clazz[]
