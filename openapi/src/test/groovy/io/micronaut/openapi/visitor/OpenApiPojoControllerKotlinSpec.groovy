package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractKotlinCompilerSpec
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse

class OpenApiPojoControllerKotlinSpec extends AbstractKotlinCompilerSpec {

    def setup() {
        Utils.clean()
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED)
        System.setProperty(Utils.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        Utils.clean()
        System.clearProperty(Utils.ATTR_TEST_MODE)
    }

    void "test kotlin"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue

@Controller("/hello")
class HelloController {

    @Get
    @Produces(MediaType.TEXT_PLAIN)
    fun index(@Nullable @QueryValue("channels") channels: Collection<Channel?>?) = ""

    @Introspected
    enum class Channel {
        @JsonProperty("mysys")
        SYSTEM1,
        SYSTEM2
    }
}

@jakarta.inject.Singleton
class MyBean
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference
        Operation operation = openApi.paths.'/hello'.get
        Schema schema = openApi.components.schemas.'HelloController.Channel'
        ApiResponse response = operation.responses.'200'

        then: "the components are valid"
        operation.parameters.size() == 1
        operation.parameters[0].name == 'channels'
        operation.parameters[0].in == 'query'
        operation.parameters[0].schema
        operation.parameters[0].schema.type == 'array'
        operation.parameters[0].schema.nullable == true
        operation.parameters[0].schema.items.nullable == true
        operation.parameters[0].schema.items.allOf[0].$ref == '#/components/schemas/HelloController.Channel'

        response.content.'text/plain'.schema.type == 'string'

        schema
        schema.type == 'string'
        schema.enum.size() == 2
        schema.enum[0] == 'mysys'
        schema.enum[1] == 'SYSTEM2'
    }

    void "test ksp jackson visitor"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import jakarta.validation.constraints.NotNull
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Controller
class HelloController {

    @Put("/sendModelWithDiscriminator")
    fun sendModelWithDiscriminator(
        @Body @NotNull @Valid animal: Animal
    ): Mono<Animal> = Mono.empty()
}

@Serdeable
@JsonIgnoreProperties(
        value = ["class"], // ignore manually set class, it will be automatically generated by Jackson during serialization
        allowSetters = true // allows the class to be set during deserialization
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "class", visible = true)
@JsonSubTypes(
        JsonSubTypes.Type(value = Bird::class, name = "ave")
)
open class Animal (
    @field:Schema(name = "color", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Nullable
    open var color: ColorEnum? = null,
    @Nullable
    open var propertyClass: String? = null,
)

@Serdeable
data class Bird (
    @Nullable
    var numWings: Int? = null,
    @Nullable
    var beakLength: BigDecimal? = null,
    @Nullable
    var featherDescription: String? = null,
): Animal()

@Serdeable
enum class ColorEnum {

    @JsonProperty("red")
    RED
}

@jakarta.inject.Singleton
class MyBean
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference
        def schemas = openApi.components.schemas

        then: "the components are valid"
        schemas.Animal
        schemas.ColorEnum
    }

    void "test kotlin NotNull on nullable types"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import jakarta.validation.constraints.NotNull
import reactor.core.publisher.Mono

@Controller
class HelloController {

    @Put("/sendModelWithDiscriminator")
    fun sendModelWithDiscriminator(
        @Body @NotNull @Valid animal: Animal
    ): Mono<Animal> = Mono.empty()

    @Get("/test{/myVar}")
    fun sendModelWithDiscriminator(
        @PathVariable myVar: String?,
        @QueryValue param1: String?
    ): String = "OK"
}

@Serdeable
data class Animal (
    @NotNull
    var color: String?,
    @NonNull
    var propertyClass: String?,
)

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas.Animal

        then: "the components are valid"
        schema
        schema.properties.size() == 2
        !schema.properties.color.nullable
        !schema.properties.propertyClass.nullable
        schema.required
        schema.required.size() == 2
        schema.required[0] == 'color'
        schema.required[1] == 'propertyClass'
    }

    void "test kotlin constructor annotations"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import reactor.core.publisher.Mono

@Controller
class HelloController {

    @Put("/sendModelWithDiscriminator")
    fun sendModelWithDiscriminator(
        @Body @NotNull @Valid animal: Animal
    ): Mono<Animal> = Mono.empty()
}

/**
 * Animal
 *
 * @param color
 * @param propertyClass
 */
@Serdeable
@JsonPropertyOrder(
        Animal.JSON_PROPERTY_PROPERTY_CLASS,
        Animal.JSON_PROPERTY_COLOR
)
open class Animal (
    @Nullable
    @Schema(name = "color", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty(JSON_PROPERTY_COLOR)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    open var color: ColorEnum? = null,
    @Size(max = 50)
    @Nullable
    @Schema(name = "class", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty(JSON_PROPERTY_PROPERTY_CLASS)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    open var propertyClass: String? = null,
) {

    companion object {

        const val JSON_PROPERTY_PROPERTY_CLASS = "class"
        const val JSON_PROPERTY_COLOR = "color"
    }
}

@Serdeable
enum class ColorEnum (
    @get:JsonValue val value: String
) {

    @JsonProperty("red")
    RED("red"),
    @JsonProperty("blue")
    BLUE("blue"),
    @JsonProperty("green")
    GREEN("green"),
    @JsonProperty("light-blue")
    LIGHT_BLUE("light-blue"),
    @JsonProperty("dark-green")
    DARK_GREEN("dark-green");

    override fun toString(): String {
        return value
    }

    companion object {

        @JvmField
        val VALUE_MAPPING = entries.associateBy { it.value }

        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): ColorEnum {
            require(VALUE_MAPPING.containsKey(value)) { "Unexpected value '$value'" }
            return VALUE_MAPPING[value]!!
        }
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas.Animal

        then: "the components are valid"
        schema
        schema.properties.size() == 2
        schema.properties.class
        schema.properties.class.maxLength == 50
    }
}
