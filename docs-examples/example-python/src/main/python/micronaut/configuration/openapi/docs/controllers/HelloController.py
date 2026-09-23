# tag::imports[]
from micronaut.http import MediaType
from micronaut.http.annotation import Controller, Get
from reactor.core.publisher import Mono
# end::imports[]
# tag::clazz[]
@Controller
class HelloController:

    @Get(uri="/hello/{name}", produces=MediaType.TEXT_PLAIN)
    def index(self, name: str) -> Mono[str]:
        """
        @param name The person's name
        @return The greeting
        """
        return Mono.just("Hello " + name + "!")
# end::clazz[]
