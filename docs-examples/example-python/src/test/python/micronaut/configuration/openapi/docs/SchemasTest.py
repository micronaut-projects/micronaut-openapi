from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


def ref(schema):
    # `Schema.get$ref()` is not a keyword clash: `$` cannot appear in a Python identifier, so the accessor
    # has no alias and must be looked up by name.
    return getattr(schema, "get$ref")()


@MicronautTest
class SchemasTest:

    @Test
    def test_schema_annotation_customizes_the_class(self):
        schemas = OpenApiSpec.load().getComponents().getSchemas()

        pet = schemas.get("MyPet")
        assert pet is not None
        assert pet.getDescription() == "Pet description"
        age = pet.getProperties().get("age")
        assert age.getDescription() == "Pet age"
        assert age.getMaximum().intValue() == 20
        name = pet.getProperties().get("name")
        assert name.getDescription() == "Pet name"
        assert name.getMaxLength() == 20
        pet_type = schemas.get("PetType")
        assert pet_type.getType() == "string"
        assert list(pet_type.getEnum()) == ["DOG", "CAT"]

    @Test
    def test_meta_annotation_applies_the_schema(self):
        operation = OpenApiSpec.load().getPaths().get("/pets").getPost()

        schema = operation.getRequestBody().getContent().get("application/json").getSchema()
        assert ref(schema) == "#/components/schemas/MyPet"

    @Test
    def test_generics_are_included_in_the_schema_name(self):
        open_api = OpenApiSpec.load()

        response = open_api.getPaths().get("/").getPut().getResponses().get("200")
        assert ref(response.getContent().get("application/json").getSchema()) == "#/components/schemas/Response_Pet_"
        schema = open_api.getComponents().getSchemas().get("Response_Pet_")
        assert ref(schema.getProperties().get("result")) == "#/components/schemas/MyPet"

    @Test
    def test_schema_name_can_be_changed(self):
        open_api = OpenApiSpec.load()

        response = open_api.getPaths().get("/named").getPut().getResponses().get("200")
        assert ref(response.getContent().get("application/json").getSchema()) == "#/components/schemas/ResponseOfPet"
        assert open_api.getComponents().getSchemas().containsKey("ResponseOfPet")
