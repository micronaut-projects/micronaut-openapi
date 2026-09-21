from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec

from io.swagger.v3.oas.models.PathItem import HttpMethod


def assert_version_parameter(parameter):
    assert parameter.getName() == "version"
    assert parameter.getIn() == "query"
    assert parameter.getDescription() == "API version"
    assert parameter.getSchema().getType() == "string"


@MicronautTest
class VersionedControllerTest:

    @Test
    def test_version_1(self):
        open_api = OpenApiSpec.load("service-1.0.0-1.yml")

        common = open_api.getPaths().get("/versioned/common").getPost()
        assert common.getOperationId() == "common"
        assert_version_parameter(common.getParameters().get(0))
        hello = open_api.getPaths().get("/versioned/hello").getGet()
        assert hello.getOperationId() == "helloV1"
        assert_version_parameter(hello.getParameters().get(0))
        assert not open_api.getPaths().get("/versioned/hello").readOperationsMap().containsKey(HttpMethod.POST)

    @Test
    def test_version_2(self):
        open_api = OpenApiSpec.load("service-1.0.0-2.yml")

        common = open_api.getPaths().get("/versioned/common").getPost()
        assert common.getOperationId() == "common"
        hello = open_api.getPaths().get("/versioned/hello").getPost()
        assert hello.getOperationId() == "helloV2"
        assert_version_parameter(hello.getParameters().get(0))
        body = hello.getRequestBody().getContent().get("application/json").getSchema()
        # `Schema.get$ref()` has no alias (`$` cannot appear in a Python identifier), so it is looked up by name
        user_dto_ref = getattr(body.getProperties().get("userDto"), "get$ref")()
        assert user_dto_ref.endswith("UserDto")
        user_dto = open_api.getComponents().getSchemas().get(user_dto_ref.removeprefix("#/components/schemas/"))
        assert "address" in list(user_dto.getRequired())
        assert list(user_dto.getProperties().keySet()) == ["name", "age", "secondName", "address"]
        assert not open_api.getPaths().get("/versioned/hello").readOperationsMap().containsKey(HttpMethod.GET)
