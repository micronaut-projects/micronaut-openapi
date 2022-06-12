package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.RequestBody
import spock.lang.IgnoreIf
import spock.lang.Issue

class OpenApiSchemaInheritanceSpec extends AbstractOpenApiTypeElementSpec {

    void "test parse the OpenAPI with response that contains generic types"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Put("/")
    @Operation(summary = "Update an existing pet")
        @RequestBody(required = true, content = @Content(
            schema = @Schema(
                    oneOf = {
                            A.class, B.class
                    },
                    discriminatorMapping = {
                            @DiscriminatorMapping(value = "A", schema = A.class),
                            @DiscriminatorMapping(value = "B", schema = B.class)
                    },
                    discriminatorProperty = "type")
    ))
    public void updatePet(Base base) {
    }
}

abstract class Base {
    private int money;

    public void setMoney(int a) {
        money = a;
    }

    public int getMoney() {
        return money;
    }
}

class A extends Base {
    private int age1;

    public void setAge1(int a) {
        age1 = a;
    }

    public int getAge1() {
        return age1;
    }
}

class B extends Base {
    private int age2;

    public void setAge2(int a) {
        age2 = a;
    }

    public int getAge2() {
        return age2;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.put
        RequestBody requestBody = operation.requestBody
        requestBody.required
        Schema schema = requestBody.content['application/json'].schema
        schema instanceof ComposedSchema
        ((ComposedSchema) schema).oneOf[0].$ref == '#/components/schemas/A'
        ((ComposedSchema) schema).oneOf[1].$ref == '#/components/schemas/B'
        ((ComposedSchema) schema).type == 'object'
        ((ComposedSchema) schema).discriminator.propertyName == 'type'
        ((ComposedSchema) schema).discriminator.mapping['A'] == '#/components/schemas/A'
        ((ComposedSchema) schema).discriminator.mapping['B'] == '#/components/schemas/B'

        expect:
        operation
        operation.responses.size() == 1
        operation.responses["200"].description == "updatePet 200 response"
        operation.responses["200"].content == null

        openAPI.components.schemas['Base'].properties['money'].type == 'integer'

        openAPI.components.schemas['A'] instanceof ComposedSchema
        ((ComposedSchema) openAPI.components.schemas['A']).allOf.size() == 2
        ((ComposedSchema) openAPI.components.schemas['A']).allOf[0].get$ref() == "#/components/schemas/Base"
        ((ComposedSchema) openAPI.components.schemas['A']).allOf[1].properties.size() == 1
        ((ComposedSchema) openAPI.components.schemas['A']).allOf[1].properties['age1'].type == "integer"

        openAPI.components.schemas['B'] instanceof ComposedSchema
        ((ComposedSchema) openAPI.components.schemas['B']).allOf.size() == 2
        ((ComposedSchema) openAPI.components.schemas['B']).allOf[0].get$ref() == "#/components/schemas/Base"
        ((ComposedSchema) openAPI.components.schemas['B']).allOf[1].properties.size() == 1
        ((ComposedSchema) openAPI.components.schemas['B']).allOf[1].properties["age2"].type == "integer"
    }

    void "test OpenAPI with body that contains nested inheritance schemas when annotation is on type"() {
        given:
            buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Post("/")
    public void updateOwner(@Body Owner owner) {
    }
}

@Introspected
@Schema(description = "Represents a person that owns a car or a bike")
class Owner {

    private Vehicle vehicle;

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}

@Introspected
@Schema(description = "Vehicle of the owner. Here a car or bike with a name", oneOf = { Car.class, Bike.class })
abstract class Vehicle {
}

@Introspected
@Schema(description = "Bike of an owner with an own name")
class Bike extends Vehicle {
}

@Introspected
@Schema(description = "Car of an owner with an own name")
class Car extends Vehicle {
}

@jakarta.inject.Singleton
class MyBean {}
''')

            OpenAPI openAPI = AbstractOpenApiVisitor.testReference
            Operation operation = openAPI.paths?.get("/")?.post
            RequestBody requestBody = operation.requestBody
            requestBody.required
            Map<String, Schema> schemas = openAPI.getComponents().getSchemas()
            Schema owner = schemas["Owner"]
            Schema vehicle = schemas["Vehicle"]

        expect:
            Schema vehicleRef = owner.getProperties()["vehicle"]
            !(vehicleRef instanceof ComposedSchema)
            vehicleRef.$ref == "#/components/schemas/Vehicle"
            vehicle instanceof ComposedSchema
            ((ComposedSchema) vehicle).oneOf[0].$ref == '#/components/schemas/Car'
            ((ComposedSchema) vehicle).oneOf[1].$ref == '#/components/schemas/Bike'
    }

    void "test OpenAPI with body that contains nested inheritance schemas when annotation is on property"() {
        given:
            buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Post("/")
    public void updateOwner(@Body Owner owner) {
    }
}

@Introspected
@Schema(description = "Represents a person that owns a car or a bike")
class Owner {

    private Vehicle vehicle;

    @Schema(description = "Vehicle of the owner. Here a car or bike with a name", oneOf = { Car.class, Bike.class })
    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}

@Introspected
abstract class Vehicle {
}

@Introspected
@Schema(description = "Bike of an owner with an own name")
class Bike extends Vehicle {
}

@Introspected
@Schema(description = "Car of an owner with an own name")
class Car extends Vehicle {
}

@jakarta.inject.Singleton
class MyBean {}
''')

            OpenAPI openAPI = AbstractOpenApiVisitor.testReference
            Operation operation = openAPI.paths?.get("/")?.post
            RequestBody requestBody = operation.requestBody
            requestBody.required
            Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
            Schema owner = schemas["Owner"]
            Schema vehicleRef = owner.getProperties()["vehicle"]
            vehicleRef instanceof ComposedSchema
            ((ComposedSchema) vehicleRef).allOf[0].$ref == "#/components/schemas/Vehicle"
            ((ComposedSchema) vehicleRef).allOf[1].description == "Vehicle of the owner. Here a car or bike with a name"
            Schema vehicle = schemas["Vehicle"]
            vehicle instanceof ComposedSchema
            ((ComposedSchema) vehicle).oneOf[0].$ref == '#/components/schemas/Car'
            ((ComposedSchema) vehicle).oneOf[1].$ref == '#/components/schemas/Bike'
    }

    void "test OpenAPI that on nested inheritance property annotation is preferred over type annotation"() {
        given:
            buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Post("/")
    public void updateOwner(@Body Owner owner) {
    }
}

@Introspected
@Schema(description = "Represents a person that owns a car or a bike")
class Owner {

    private Vehicle vehicle;

    @Schema(name = "Owner.Vehicle", description = "Vehicle of the owner. Here a car or bike with a name", oneOf = { Car.class, Bike.class })
    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}

@Introspected
@Schema(description = "Vehicle of the owner.")
abstract class Vehicle {
}

@Introspected
@Schema(description = "Bike of an owner with an own name")
class Bike extends Vehicle {
}

@Introspected
@Schema(description = "Car of an owner with an own name")
class Car extends Vehicle {
}

@jakarta.inject.Singleton
class MyBean {}
''')

            OpenAPI openAPI = AbstractOpenApiVisitor.testReference
            Operation operation = openAPI.paths?.get("/")?.post
            RequestBody requestBody = operation.requestBody
            requestBody.required
            Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
            Schema owner = schemas["Owner"]
            Schema vehicleRef = owner.getProperties()["Owner.Vehicle"]
            vehicleRef instanceof ComposedSchema
            ((ComposedSchema) vehicleRef).allOf[0].$ref == "#/components/schemas/Owner.Vehicle"
            ((ComposedSchema) vehicleRef).allOf[1].description == "Vehicle of the owner. Here a car or bike with a name"
            Schema ownerVehicle = schemas["Owner.Vehicle"]
            ((ComposedSchema) ownerVehicle).oneOf[0].$ref == '#/components/schemas/Car'
            ((ComposedSchema) ownerVehicle).oneOf[1].$ref == '#/components/schemas/Bike'
    }

    void "test OpenAPI with body that contains nested inheritance schemas apply additional information to schema"() {
        given:
            buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Post("/")
    public void updateOwner(@Body Owner owner) {
    }
}

@Introspected
@Schema(description = "Represents a person that owns a car or a bike")
class Owner {

    private Vehicle vehicle;

    /**
     * Some docs
     */
    @Nullable
    @Deprecated
    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}

@Introspected
@Schema(description = "Base vehicle", oneOf = { Car.class, Bike.class })
abstract class Vehicle {
}

@Introspected
@Schema(description = "Bike of an owner with an own name")
class Bike extends Vehicle {
}

@Introspected
@Schema(description = "Car")
class Car extends Vehicle {
}

@jakarta.inject.Singleton
class MyBean {}
''')

            OpenAPI openAPI = AbstractOpenApiVisitor.testReference
            Operation operation = openAPI.paths?.get("/")?.post
            RequestBody requestBody = operation.requestBody
            requestBody.required
            Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
            Schema owner = schemas["Owner"]
            Schema vehicleProperty = owner.getProperties()["vehicle"]
            vehicleProperty instanceof ComposedSchema
            ((ComposedSchema) vehicleProperty).deprecated
            ((ComposedSchema) vehicleProperty).description == "Some docs"
            ((ComposedSchema) vehicleProperty).nullable
            ((ComposedSchema) vehicleProperty).oneOf[0].$ref == "#/components/schemas/Vehicle"
            Schema vehicle = schemas["Vehicle"]
            vehicle instanceof ComposedSchema
            ((ComposedSchema) vehicle).oneOf[0].$ref == '#/components/schemas/Car'
            ((ComposedSchema) vehicle).oneOf[1].$ref == '#/components/schemas/Bike'
    }

    @IgnoreIf({ !jvm.isJava16Compatible() })
    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/659")
    void "test OpenAPI proper inheritance of nullable, description and required attributes"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Introspected
@Schema(description = "Schema that represents the possible email protocols for sending emails")
enum EmailSendProtocolDto {
    SMTP,
    SMTP_SSL,
    SMTP_STARTTLS
}

@Introspected
@Schema(description = "Schema that represents the current email settings")
record ReadEmailSettingsDto(

        @Schema(description = "Flag that indicates whether the email sending is active or not. If set to false, all other values are null", required = true)
        Boolean active,

        @Schema(description = "Hostname or IP of the email server or null if email sending is disabled", required = true, nullable = true)
        String hostname,

        @Schema(description = "Port of the email server or null if email sending is disabled", required = true, nullable = true)
        Integer port,

        @Schema(description = "Protocol used for the connection or null if email sending is disabled", required = true, nullable = true)
        EmailSendProtocolDto protocol,

        @Schema(description = "Email username to login or null if email sending is disabled", required = true, nullable = true)
        String username,

        @Schema(description = "Plaintext password for the email user to login in or null if email sending is disabled", required = true, nullable = true)
        String plaintextPassword,

        @Schema(description = "Sender email address that is used to send emails or null if email sending is disabled", required = true, nullable = true)
        String senderEmail
) {
}

@Introspected
@Schema(description = "Schema that represents an existing email output location")
record ReadEmailOutputLocationDto(

        // Snipped a lot of attributes. Class doesn't make any sense for outstanders

        @Schema(description = "Protocol used for the connection", required = true)
        EmailSendProtocolDto protocol
) {
}

@Controller("/api")
class EmailController {

    /**
     * Get the email settings.
     *
     * @return Email settings
     * @throws Exception Exception in case of invalid data or an issue with reading the settings
     */
    @Get("/email/settings")
    public ReadEmailSettingsDto getEmailSettings(){
        return null;
    }

    /**
     * Get the email output location.
     *
     * @return Email output location
     * @throws Exception Exception in case of invalid data or an issue with reading the settings
     */
    @Get("/email/output")
    public ReadEmailOutputLocationDto getEmailOutputLocation() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
        Schema EmailSendProtocolDtoSchema = schemas["EmailSendProtocolDto"]

        EmailSendProtocolDtoSchema.description == "Schema that represents the possible email protocols for sending emails"
        !EmailSendProtocolDtoSchema.nullable
        !EmailSendProtocolDtoSchema.required

        schemas["ReadEmailOutputLocationDto"].required.containsAll(["protocol"])
        Schema emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto = schemas["ReadEmailOutputLocationDto"].getProperties()["protocol"]
        emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto instanceof ComposedSchema
        ((ComposedSchema) emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto).allOf[0].$ref == "#/components/schemas/EmailSendProtocolDto"
        ((ComposedSchema) emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto).allOf[1].description == "Protocol used for the connection"
        !((ComposedSchema) emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto).nullable
        !((ComposedSchema) emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto).required

        schemas["ReadEmailSettingsDto"].required.containsAll(["protocol", "active", "hostname", "port", "senderEmail", "username", "plaintextPassword"])
        Schema emailSendProtocolDtoSchemaFromReadEmailSettingsDto = schemas["ReadEmailSettingsDto"].getProperties()["protocol"]
        emailSendProtocolDtoSchemaFromReadEmailSettingsDto instanceof ComposedSchema
        ((ComposedSchema) emailSendProtocolDtoSchemaFromReadEmailSettingsDto).allOf[0].$ref == "#/components/schemas/EmailSendProtocolDto"
        ((ComposedSchema) emailSendProtocolDtoSchemaFromReadEmailSettingsDto).allOf[1].description == "Protocol used for the connection or null if email sending is disabled"
        ((ComposedSchema) emailSendProtocolDtoSchemaFromReadEmailSettingsDto).nullable
        !((ComposedSchema) emailSendProtocolDtoSchemaFromReadEmailSettingsDto).required
    }

}
