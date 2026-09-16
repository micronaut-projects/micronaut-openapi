package io.micronaut.configuration.openapi.docs.versions;

// tag::imports[]
import jakarta.validation.constraints.NotNull;
// end::imports[]

// tag::clazz[]
public class UserDto {

    public String name;
    public int age;
    public String secondName;
    @NotNull
    public String address;
}
// end::clazz[]
