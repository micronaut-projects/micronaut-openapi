from typing import Annotated

# tag::imports[]
from micronaut.http import MediaType
from micronaut.http.annotation import Controller, Get
from jakarta.validation.constraints import NotBlank
from reactor.core.publisher import Mono

from io.swagger.v3.oas.annotations import Operation, Parameter
from io.swagger.v3.oas.annotations.media import Content, Schema
from io.swagger.v3.oas.annotations.responses import ApiResponse
from io.swagger.v3.oas.annotations.tags import Tag
# end::imports[]


# tag::clazz[]
@Controller
class HelloController:

    @Get(uri="/greetings/{name}", produces=MediaType.TEXT_PLAIN)
    @Operation(summary="Greets a person", description="A friendly greeting is returned")
    @ApiResponse(content=Content(mediaType="text/plain", schema=Schema(type="string")))
    @ApiResponse(responseCode="400", description="Invalid Name Supplied")
    @ApiResponse(responseCode="404", description="Person not found")
    @Tag(name="greeting")
    def greetings(self, name: Annotated[str, Parameter(description="The name of the person"), NotBlank]) -> Mono[str]:
        """
        @param name The person's name
        @return The greeting message
        """
        return Mono.just("Hello " + name + ", How are you doing?")
# end::clazz[]
