package io.micronaut.configuration.openapi.docs.filters

// tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import java.time.Duration
// end::imports[]

// tag::clazz[]
@Requires(property = "micronaut.server.context-path")
@Filter(methods = [HttpMethod.GET, HttpMethod.HEAD], patterns = ["/**/rapidoc*", "/**/redoc*", "/**/swagger-ui*", "/**/openapi-explorer*"])
class OpenApiViewCookieContextPathFilter(@Value("\${micronaut.server.context-path}") contextPath: String) : HttpServerFilter {

    private val contextPathCookie: Cookie = Cookie.of("contextPath", contextPath).maxAge(Duration.ofMinutes(2L))

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        return Publishers.map(chain.proceed(request)) { response -> response.cookie(contextPathCookie) }
    }
}
// end::clazz[]
