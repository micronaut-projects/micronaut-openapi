package io.micronaut.configuration.openapi.docs.schemas

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema
// end::imports[]

// tag::clazz[]
@Schema(name="MyPet", description="Pet description") // <1>
class Pet {

    private PetType type
    private int age
    private String name

    void setAge(int a) {
        age = a
    }

    /**
     * The age
     */
    @Schema(description="Pet age", maximum="20") // <2>
    int getAge() {
        return age
    }

    void setName(String n) {
        name = n
    }

    @Schema(description="Pet name", maxLength=20)
    String getName() {
        return name
    }

    void setType(PetType t) {
        type = t
    }

    PetType getType() {
        return type
    }
}
// end::clazz[]
