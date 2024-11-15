package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.RequestBody
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

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.put
        RequestBody requestBody = operation.requestBody
        requestBody.required
        Schema schema = requestBody.content['application/json'].schema
        schema.oneOf[0].$ref == '#/components/schemas/A'
        schema.oneOf[1].$ref == '#/components/schemas/B'
        schema.type == 'object'
        schema.discriminator.propertyName == 'type'
        schema.discriminator.mapping['A'] == '#/components/schemas/A'
        schema.discriminator.mapping['B'] == '#/components/schemas/B'

        expect:
        operation
        operation.responses.size() == 1
        operation.responses["200"].description == "updatePet 200 response"
        operation.responses["200"].content == null

        openAPI.components.schemas['Base'].properties['money'].type == 'integer'

        openAPI.components.schemas['A'].allOf.size() == 2
        openAPI.components.schemas['A'].allOf[0].get$ref() == "#/components/schemas/Base"
        openAPI.components.schemas['A'].allOf[1].properties.size() == 1
        openAPI.components.schemas['A'].allOf[1].properties['age1'].type == "integer"

        openAPI.components.schemas['B'].allOf.size() == 2
        openAPI.components.schemas['B'].allOf[0].get$ref() == "#/components/schemas/Base"
        openAPI.components.schemas['B'].allOf[1].properties.size() == 1
        openAPI.components.schemas['B'].allOf[1].properties["age2"].type == "integer"
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

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.post
        RequestBody requestBody = operation.requestBody
        requestBody.required
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas()
        Schema owner = schemas["Owner"]
        Schema vehicle = schemas["Vehicle"]

        expect:
        Schema vehicleRef = owner.getProperties()["vehicle"]
        vehicleRef.$ref == "#/components/schemas/Vehicle"
        vehicle
        vehicle.oneOf
        vehicle.oneOf[0].$ref == '#/components/schemas/Car'
        vehicle.oneOf[1].$ref == '#/components/schemas/Bike'
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

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.post
        RequestBody requestBody = operation.requestBody
        requestBody.required
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
        Schema owner = schemas.Owner
        Schema vehicleRef = owner.properties.vehicle
        vehicleRef.description == "Vehicle of the owner. Here a car or bike with a name"
        vehicleRef.allOf[0].$ref == "#/components/schemas/Vehicle"
        vehicleRef.oneOf[0].$ref == '#/components/schemas/Car'
        vehicleRef.oneOf[1].$ref == '#/components/schemas/Bike'
        Schema vehicle = schemas["Vehicle"]
        vehicle.type == 'object'
    }

    void "test OpenAPI that on nested inheritance property annotation is preferred over type annotation"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.media.Schema;

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

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.post
        RequestBody requestBody = operation.requestBody
        requestBody.required
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
        Schema owner = schemas.Owner
        Schema vehicleRef = owner.properties."Owner.Vehicle"
        vehicleRef.description == "Vehicle of the owner. Here a car or bike with a name"
        vehicleRef.allOf[0].$ref == "#/components/schemas/Vehicle"
        Schema ownerVehicle = schemas.Vehicle
        ownerVehicle.oneOf[0].$ref == '#/components/schemas/Car'
        ownerVehicle.oneOf[1].$ref == '#/components/schemas/Bike'
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

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.post
        RequestBody requestBody = operation.requestBody
        requestBody.required
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
        Schema owner = schemas["Owner"]
        Schema vehicleProperty = owner.getProperties()["vehicle"]
        vehicleProperty.deprecated
        vehicleProperty.description == "Some docs"
        vehicleProperty.nullable
        vehicleProperty.allOf[0].$ref == "#/components/schemas/Vehicle"
        Schema vehicle = schemas["Vehicle"]
        vehicle.oneOf[0].$ref == '#/components/schemas/Car'
        vehicle.oneOf[1].$ref == '#/components/schemas/Bike'
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/659")
    void "test OpenAPI proper inheritance of nullable, description and required attributes"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;

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

        OpenAPI openAPI = Utils.testReference
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
        Schema EmailSendProtocolDtoSchema = schemas["EmailSendProtocolDto"]

        EmailSendProtocolDtoSchema.description == "Schema that represents the possible email protocols for sending emails"
        !EmailSendProtocolDtoSchema.nullable
        !EmailSendProtocolDtoSchema.required

        schemas["ReadEmailOutputLocationDto"].required.containsAll(["protocol"])
        Schema emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto = schemas.ReadEmailOutputLocationDto.properties.protocol
        emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto.description == "Protocol used for the connection"
        emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto.allOf[0].$ref == "#/components/schemas/EmailSendProtocolDto"
        !emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto.nullable
        !emailSendProtocolDtoSchemaFromReadEmailOutputLocationDto.required

        schemas["ReadEmailSettingsDto"].required.containsAll(["protocol", "active", "hostname", "port", "senderEmail", "username", "plaintextPassword"])
        Schema emailSendProtocolDtoSchemaFromReadEmailSettingsDto = schemas.ReadEmailSettingsDto.properties.protocol
        emailSendProtocolDtoSchemaFromReadEmailSettingsDto.description == "Protocol used for the connection or null if email sending is disabled"
        emailSendProtocolDtoSchemaFromReadEmailSettingsDto.allOf[0].$ref == "#/components/schemas/EmailSendProtocolDto"
        emailSendProtocolDtoSchemaFromReadEmailSettingsDto.nullable
        !emailSendProtocolDtoSchemaFromReadEmailSettingsDto.required
    }

    void "test OpenAPI with inheritance and allOf together in complex openApi"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Controller
class MyController {

    @Post("/bike")
    public void bike(@Body Owner owner) {
    }

    @Post("/car")
    public void car(@Body Car car) {
    }
}

@Introspected
@Schema(description = "Represents a person that owns a car or a bike")
class Owner {

    /**
     * The bike
     */
    @NotNull
    public Bike bike;
    /**
     * The car
     */
    @NotNull
    public Car car;
}

@Introspected
@Schema(description = "Vehicle of the owner.")
abstract class Vehicle<T> {

    private List<T> vehicleField;

    /**
     * Getter javadoc.
     */
    @ArraySchema(schema = @Schema(implementation = Document.class, description = "List of objects"))
    @NotNull
    public List<T> getVehicleField() {
        return vehicleField;
    }

    public void setVehicleField(List<T> vehicleField) {
        this.vehicleField = vehicleField;
    }
}

/**
 * Bike schema.
 */
@Introspected
class Bike extends Vehicle<MyDocument> {

    /**
     * Child getter javadoc.
     */
    @Size(min = 10, max = 20)
    @NotEmpty
    @ArraySchema(schema = @Schema(implementation = MyDocument.class))
    @Override
    public List<MyDocument> getVehicleField() {
        return super.getVehicleField();
    }
}

/**
 * Car schema.
 */
@Introspected
class Car extends Vehicle<AverageStats> {

    private String carField;

    /**
     * Child getter javadoc.
     */
    @ArraySchema(schema = @Schema(implementation = AverageStats.class))
    @Override
    public List<AverageStats> getVehicleField() {
        return super.getVehicleField();
    }

    /**
     * Getter javadoc.
     */
    public String getCarField() {
        return carField;
    }

    public void setCarField(String carField) {
        this.carField = carField;
    }
}

/**
 * AverageStats class javadoc.
 */
@Schema(allOf = Document.class)
class AverageStats extends DocumentImpl {

    /**
     * Average value
     */
    @NotNull
    private Double statValue;
    /**
     * The second metric.
     */
    @NotNull
    private Long secondMetric;

    public Double getStatValue() {
        return statValue;
    }

    public void setStatValue(Double statValue) {
        this.statValue = statValue;
    }

    public Long getSecondMetric() {
        return secondMetric;
    }

    public void setSecondMetric(Long secondMetric) {
        this.secondMetric = secondMetric;
    }
}

/**
 * MyDocument class javadoc.
 */
@Schema(allOf = Document.class)
class MyDocument extends DocumentImpl {

    /**
     * NII field
     */
    @NotNull
    private Long nii;
    /**
     * Field2 description
     */
    @NotNull
    private Long field2;

    public Long getField2() {
        return field2;
    }

    public void setField2(Long field2) {
       this.field2 = field2;
    }

    public Long getNii() {
        return nii;
    }

    public void setNii(Long nii) {
       this.nii = nii;
    }
}

/**
 * DocumentImpl class javadoc.
 */
class DocumentImpl implements Document, Serializable {

    @Schema(description = "Unique id", required = true)
    private String id;

    @Schema(hidden = true)
    @Hidden
    private Boolean hidden;

    @Schema(description = "Modification date in UnixTime",
            implementation = long.class,
            accessMode = Schema.AccessMode.READ_ONLY)
    private long modified;

    @Schema(description = "Schema name",
            implementation = String.class,
            accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(required = true)
    private String schema;

    @Hidden
    @JsonIgnore
    private Set<DocumentProperty> properties;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Hidden
    @JsonIgnore
    public Boolean isHidden() {
        return hidden;
    }

    @Schema(hidden = true)
    @JsonProperty
    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    @JsonProperty
    @Override
    public long getModified() {
        return modified;
    }

    @JsonProperty
    @Override
    public @NotNull String getSchema() {
        return null;
    }

    @JsonProperty
    public Document setSchema(String schema) {
        this.schema = schema;
        return this;
    }

    @JsonIgnore
    @Override
    public final @Nullable Object getExpand(String field) {
        return null;
    }

    @JsonAnyGetter
    @NotNull
    @Override
    public final Map<String, Object> getExpands() {
        return Collections.emptyMap();
    }

    @JsonAnySetter
    @Override
    public final void setExpand(@Nullable String field, @Nullable Object value) {
    }
}

enum DocumentProperty {
    READ_ONLY
}

interface Document {

    /**
     * @return document identified
     */
    String getId();

    /**
     * @return document modified time as UnixTime
     */
    long getModified();

    /**
     * @return document schema
     */
    @NotNull
    String getSchema();

    /**
     * @param field to get value
     * @return specified field value
     */
    @Nullable
    Object getExpand(String field);

    /**
     * @return retrieve all documents expand fields
     */
    @NotNull
    Map<String, Object> getExpands();

    /**
     * @param field to set value to
     * @param value to set for field
     */
    void setExpand(@Nullable String field, @Nullable Object value);
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        def schemas = openAPI.getComponents().getSchemas()

        expect:
        schemas
        schemas.size() == 9

        def averageStats = schemas.AverageStats
        averageStats.allOf.size() == 3
        averageStats.allOf.get(0).$ref == '#/components/schemas/Document'
        averageStats.allOf.get(1).$ref == '#/components/schemas/DocumentImpl'
        averageStats.allOf.get(2).$ref == null

        def bike = schemas.Bike
        bike.allOf.size() == 2
        bike.allOf.get(0).$ref == '#/components/schemas/Vehicle_MyDocument_'
        bike.allOf.get(1).$ref == null
        bike.allOf.get(1).properties.size() == 1
        bike.allOf.get(1).properties.vehicleField.description == 'Child getter javadoc.'

        def car = schemas.Car
        car.allOf.size() == 2
        car.allOf.get(0).$ref == '#/components/schemas/Vehicle_AverageStats_'
        car.allOf.get(1).$ref == null
        car.allOf.get(1).properties.size() == 2
        car.allOf.get(1).properties.vehicleField.description == 'Child getter javadoc.'
        car.allOf.get(1).properties.carField.description == 'Getter javadoc.'

        def document = schemas.Document
        document.properties.id
        document.properties.modified
        document.properties.schema
        document.properties.expands
        document.properties.expands.additionalProperties == true

        def myDocument = schemas.MyDocument
        myDocument.allOf.size() == 3
        myDocument.allOf.get(0).$ref == '#/components/schemas/Document'
        myDocument.allOf.get(1).$ref == '#/components/schemas/DocumentImpl'
        myDocument.allOf.get(2).$ref == null
    }
}
