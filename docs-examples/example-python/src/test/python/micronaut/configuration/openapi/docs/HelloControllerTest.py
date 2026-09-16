from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


@MicronautTest
class HelloControllerTest:

    @Test
    def test_open_api_definition(self):
        open_api = OpenApiSpec.load()

        info = open_api.getInfo()
        assert info.getTitle() == "Hello World"
        assert info.getDescription() == "My API"
        assert info.getVersion() == "0.0"
        assert info.getContact().getName() == "Fred"
        assert info.getContact().getUrl() == "https://gigantic-server.com"
        assert info.getContact().getEmail() == "Fred@gigagantic-server.com"
        assert info.getLicense().getName() == "Apache 2.0"
        assert info.getLicense().getUrl() == "https://foo.bar"

    @Test
    def test_docstring_fills_the_descriptions(self):
        operation = OpenApiSpec.load().getPaths().get("/hello/{name}").getGet()

        assert operation.getOperationId().startswith("index")
        parameter = operation.getParameters().get(0)
        assert parameter.getName() == "name"
        assert parameter.getIn() == "path"
        assert parameter.getDescription() == "The person's name"
        assert parameter.getRequired()
        assert parameter.getSchema().getType() == "string"
        response = operation.getResponses().get("200")
        assert response.getDescription() == "The greeting"
        assert response.getContent().get("text/plain").getSchema().getType() == "string"
