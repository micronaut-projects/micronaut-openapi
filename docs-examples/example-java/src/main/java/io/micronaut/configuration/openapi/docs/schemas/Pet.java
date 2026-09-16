package io.micronaut.configuration.openapi.docs.schemas;

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema;
// end::imports[]

// tag::clazz[]
@Schema(name="MyPet", description="Pet description") // <1>
public class Pet {

    private PetType type;
    private int age;
    private String name;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    @Schema(description="Pet age", maximum="20") // <2>
    public int getAge() {
        return age;
    }

    public void setName(String n) {
        name = n;
    }

    @Schema(description="Pet name", maxLength=20)
    public String getName() {
        return name;
    }

    public void setType(PetType t) {
        type = t;
    }

    public PetType getType() {
        return type;
    }
}
// end::clazz[]
