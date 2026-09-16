from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


@MicronautTest
class SwaggerAnnotationsTest:

    @Test
    def test_swagger_annotations_take_precedence(self):
        operation = OpenApiSpec.load().getPaths().get("/greetings/{name}").getGet()

        assert list(operation.getTags()) == ["greeting"]
        assert operation.getSummary() == "Greets a person"
        assert operation.getDescription() == "A friendly greeting is returned"
        assert operation.getOperationId() == "greetings"
        parameter = operation.getParameters().get(0)
        assert parameter.getName() == "name"
        assert parameter.getIn() == "path"
        assert parameter.getDescription() == "The name of the person"
        assert parameter.getRequired()
        assert parameter.getSchema().getMinLength() == 1
        assert parameter.getSchema().getType() == "string"
        responses = operation.getResponses()
        assert responses.get("200").getContent().get("text/plain").getSchema().getType() == "string"
        assert responses.get("400").getDescription() == "Invalid Name Supplied"
        assert responses.get("404").getDescription() == "Person not found"
