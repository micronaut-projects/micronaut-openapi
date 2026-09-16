from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


@MicronautTest
class OpenApiIncludeTest:

    @Test
    def test_compiled_controllers_are_included(self):
        paths = OpenApiSpec.load().getPaths()

        login = paths.get("/login").getPost()
        assert list(login.getTags()) == ["Security"]
        assert paths.get("/logout") is not None
        env = paths.get("/env").getGet()
        assert list(env.getTags()) == ["Management"]
        assert list(env.getSecurity().get(0).get("BEARER")) == ["ADMIN"]
