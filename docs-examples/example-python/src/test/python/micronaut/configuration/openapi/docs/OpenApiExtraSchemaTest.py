from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


@MicronautTest
class OpenApiExtraSchemaTest:

    @Test
    def test_extra_schemas_are_added(self):
        schemas = OpenApiSpec.load().getComponents().getSchemas()

        assert schemas.containsKey("UnusedSchema")
        assert schemas.containsKey("UnusedModel1")
        assert schemas.containsKey("ExtraModel")
        assert not schemas.containsKey("ExcludedModel")
        assert not schemas.containsKey("ExcludedByPackage")
