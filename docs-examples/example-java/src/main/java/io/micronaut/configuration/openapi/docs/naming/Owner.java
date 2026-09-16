package io.micronaut.configuration.openapi.docs.naming;

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema;
// end::imports[]

// tag::clazz[]
class Owner {

    private Pet bird;
    private Pet cat;
    private Pet dog;

    @Schema(description = "Pet that is a bird") // <2>
    public Pet getBird() {
        return bird;
    }

    @Schema(description = "Pet that is a cat") // <3>
    public Pet getCat() {
        return cat;
    }

    @Schema(name = "Dog", description = "Pet that is a dog") // <4>
    public Pet getDog() {
        return dog;
    }
}
// end::clazz[]
