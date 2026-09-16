from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec


@MicronautTest
class AccessorsStyleTest:

    @Test
    def test_attributes_are_detected(self):
        person = OpenApiSpec.load().getComponents().getSchemas().get("Person")

        assert list(person.getProperties().keySet()) == ["name", "debtValue", "totalGoals"]
        assert person.getProperties().get("name").getType() == "string"
        assert person.getProperties().get("debtValue").getType() == "integer"
        assert person.getProperties().get("totalGoals").getType() == "integer"
