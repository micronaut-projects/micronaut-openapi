from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


@MicronautTest
class OpenApiDecoratorTest:

    @Test
    def test_operation_ids_are_prefixed_and_suffixed(self):
        paths = OpenApiSpec.load().getPaths()

        assert paths.get("/cats").getPost().getOperationId() == "cats-save-suffix"
        assert paths.get("/cats/{id}").getGet().getOperationId() == "cats-get-suffix"
        assert paths.get("/dogs").getPost().getOperationId() == "dogs-save"
        assert paths.get("/dogs/{id}").getGet().getOperationId() == "dogs-get"
