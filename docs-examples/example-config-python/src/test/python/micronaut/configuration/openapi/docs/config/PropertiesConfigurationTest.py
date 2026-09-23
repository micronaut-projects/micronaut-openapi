from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


@MicronautTest
class PropertiesConfigurationTest:

    @Test
    def test_placeholders_are_expanded(self):
        info = OpenApiSpec.load("hello-world-v1.1.yml").getInfo()

        assert info.getTitle() == "Hello World"
        assert info.getDescription() == "A nice API"
        assert info.getVersion() == "v1.1"
        assert info.getContact().getName() == "Fred"
        assert info.getLicense().getName() == "Apache 2.0"
