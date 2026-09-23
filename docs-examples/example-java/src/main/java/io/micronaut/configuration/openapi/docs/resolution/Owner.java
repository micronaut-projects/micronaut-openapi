package io.micronaut.configuration.openapi.docs.resolution;

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema;
// end::imports[]

// tag::clazz[]
class Owner {

    private Pet cat;
    private Pet dog;

    public Pet getCat() { // <2>
        return cat;
    }

    @Schema(name="MyPet", description="This is my pet") // <3>
    public Pet getDog() {
        return dog;
    }

}
// end::clazz[]
