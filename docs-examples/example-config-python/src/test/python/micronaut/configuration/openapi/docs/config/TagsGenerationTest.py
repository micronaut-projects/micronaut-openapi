from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


@MicronautTest
class TagsGenerationTest:

    @Test
    def test_tags_are_generated_from_the_class_name(self):
        open_api = OpenApiSpec.load("hello-world-v1.1.yml")

        tag = open_api.getTags().get(0)
        assert tag.getName() == "user-operations"
        assert tag.getDescription() == "User main operations."
        paths = open_api.getPaths()
        assert list(paths.get("/read/{id}").getGet().getTags()) == ["user-operations"]
        assert paths.get("/read/{id}").getGet().getOperationId() == "read"
        assert list(paths.get("/save/{id}").getPost().getTags()) == ["user-operations"]
        assert paths.get("/save/{id}").getPost().getOperationId() == "save2"
        assert list(paths.get("/save").getPost().getTags()) == ["user-operations"]
        assert paths.get("/save").getPost().getOperationId() == "save"
