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
@Filter(
    methods = [HttpMethod.GET, HttpMethod.HEAD],
    patterns = ["/**/rapidoc*", "/**/redoc*", "/**/swagger-ui*", "/**/openapi-explorer*"]
)
@Requires(property = "micronaut.server.context-path-header")
class OpenApiContextPathFilter(@Value("\${micronaut.server.context-path-header}") private val contextPathHeader: String) : HttpServerFilter {

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val contextPath = request.headers[contextPathHeader]

        return if (contextPath != null) {
            val contextPathCookie = Cookie.of("contextPath", contextPath).maxAge(Duration.ofMinutes(2L))
            Publishers.map(chain.proceed(request)) { response -> response.cookie(contextPathCookie) }
        } else {
            chain.proceed(request)
        }
    }
}
// end::clazz[]
