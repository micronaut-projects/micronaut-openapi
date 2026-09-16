from typing import Annotated

# tag::imports[]
from java.time import Duration
from micronaut.context.annotation import Requires, Value
from micronaut.core.async_.publisher import Publishers
from micronaut.http import HttpMethod, HttpRequest, MutableHttpResponse
from micronaut.http.annotation import Filter
from micronaut.http.cookie import Cookie
from micronaut.http.filter import HttpServerFilter, ServerFilterChain
from org.reactivestreams import Publisher
# end::imports[]


# tag::clazz[]
@Filter(
    methods=[HttpMethod.GET, HttpMethod.HEAD],
    patterns=["/**/rapidoc*", "/**/redoc*", "/**/swagger-ui*", "/**/openapi-explorer*"],
)
@Requires(property="micronaut.server.context-path-header")
class OpenApiContextPathFilter(HttpServerFilter):

    def __init__(self, contextPathHeader: Annotated[str, Value("${micronaut.server.context-path-header}")]):
        self.contextPathHeader = contextPathHeader

    def doFilter(self, request: HttpRequest, chain: ServerFilterChain) -> Publisher[MutableHttpResponse]:
        contextPath = request.getHeaders().get(self.contextPathHeader)

        if contextPath is not None:
            contextPathCookie = Cookie.of("contextPath", contextPath).maxAge(Duration.ofMinutes(2))
            return Publishers.map(chain.proceed(request), lambda response: response.cookie(contextPathCookie))
        else:
            return chain.proceed(request)
# end::clazz[]
