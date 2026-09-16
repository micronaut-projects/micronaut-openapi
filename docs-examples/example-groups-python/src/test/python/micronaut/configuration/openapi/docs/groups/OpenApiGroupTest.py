from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


@MicronautTest
class OpenApiGroupTest:

    @Test
    def test_group_v1(self):
        open_api = OpenApiSpec.load("public-api-v1-v1-v1.yml")

        assert open_api.getInfo().getTitle() == "Public api v1"
        assert open_api.getInfo().getVersion() == "v1"
        assert open_api.getInfo().getDescription() == "This is API version 1"
        paths = open_api.getPaths()
        assert paths.containsKey("/save")
        assert paths.containsKey("/read/{id}")
        assert not paths.containsKey("/save/{id}")

    @Test
    def test_group_v2(self):
        open_api = OpenApiSpec.load("public-api-v2-v2-v2.yml")

        assert open_api.getInfo().getTitle() == "Public api v2"
        assert open_api.getInfo().getVersion() == "v2"
        paths = open_api.getPaths()
        assert paths.containsKey("/save")
        assert paths.containsKey("/save/{id}")
        assert not paths.containsKey("/read/{id}")
