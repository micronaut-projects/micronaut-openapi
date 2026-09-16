from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


@MicronautTest
class OpenApiExcludeTest:

    @Test
    def test_excluded_controllers_are_not_documented(self):
        paths = OpenApiSpec.load().getPaths()

        assert not paths.containsKey("/old")
        assert not paths.containsKey("/internal")
