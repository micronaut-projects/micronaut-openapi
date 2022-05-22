package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class OpenApiJsonUnwrappedsSpec extends AbstractOpenApiTypeElementSpec {

    void "test JsonUnwrapped annotation"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Controller("/test")
interface TestOperations {

    @Post("/")
    Single<Test> save(String name, int age);
}

class Dummy {
    public int aa;
    @JsonProperty(value = "cc", required = true)
    public String bb;
}

@Schema(description = "Represents a pet")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class Pet {
    @Schema(name="pet-name", description = "The pet name")
    private String name;
    private String lastName;
    private Integer age;

    @Schema(name="pet-name", description = "The pet name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Schema(name="pet-age", description = "The pet age")
    public Integer getAge() {
        return age;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
}

@Schema
class Test {
    public Dummy plain;

    @JsonUnwrapped
    public Dummy unwrapped;

    @JsonUnwrapped(prefix = "aaa", suffix = "zzz")
    public Dummy unwrappedRenamed;

    @JsonUnwrapped(enabled = false)
    public Dummy unwrappedDisabled;

    @JsonUnwrapped
    public Pet pet;

    @JsonUnwrapped(prefix = "aaa", suffix = "zzz")
    public Pet petRenamed;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema schema = openAPI.components.schemas['Test']
        Schema dummySchema = openAPI.components.schemas['Dummy']
        Schema petSchema = openAPI.components.schemas['Pet']

        then:"the components are valid"
        schema.type == 'object'
        schema.properties.size() == 12
        schema.properties['plain'].$ref == '#/components/schemas/Dummy'
        schema.properties['unwrappedDisabled'].$ref == '#/components/schemas/Dummy'
        schema.properties['aa'].type == 'integer'
        schema.properties['bb'] == null
        schema.properties['cc'].type == 'string'
        schema.properties['aaaaazzz'].type == 'integer'
        schema.properties['aaabbzzz'] == null
        schema.properties['aaacczzz'].type == 'string'
        schema.properties['pet-name'].type == 'string'
        schema.properties['pet-age'].type == 'integer'
        schema.properties['last_name'].type == 'string'
        schema.properties['aaapet-namezzz'].type == 'string'
        schema.properties['aaapet-agezzz'].type == 'integer'
        schema.properties['aaalast_namezzz'].type == 'string'
        schema.required == ['aaacczzz', 'cc']
        dummySchema.properties['bb'] == null
        dummySchema.properties['cc'].type == 'string'
        dummySchema.required == ['cc']
        petSchema.properties['last_name'].type == 'string'
        petSchema.required == null
    }
}
