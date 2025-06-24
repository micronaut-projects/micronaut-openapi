package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec

class OpenApiOneOfSchemaSpec extends AbstractOpenApiTypeElementSpec {

    void "test oneOf with inheritance and duplicated elements"() {

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.time.LocalDate;

@Controller("/demo")
class DemoController {

    @Get(uri = "/")
    public RecurringData index() {
        return null;
    }
}

@Serdeable
class RecurringData {
    public String scheduleName = null;

    @Valid
    public RecurringDataFrequency frequency = null;
}

@Serdeable
@JsonTypeName("RecurringDataFrequency")
class RecurringDataFrequency {

    @Schema(name = "oneOf", title = "OneOf",
            implementation = RecurringDataFrequencyOneOf.class,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            oneOf = {DeadlineFrequency.class, QuantityFrequency.class})
    public RecurringDataFrequencyOneOf oneOf = null;
}

class QuantityFrequency  implements RecurringDataFrequencyOneOf {

    @Introspected
    enum FrequencyTypeEnum {
        QUANTITY("QUANTITY");
    
        private String value;
    
        FrequencyTypeEnum(String value) {
            this.value = value;
        }
    
        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    
        @JsonCreator
        public static FrequencyTypeEnum fromValue(String text) {
            for (FrequencyTypeEnum b : FrequencyTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }
            
    @Schema(name="frequencyType",example = "QUANTITY", implementation = String.class, requiredMode=Schema.RequiredMode.NOT_REQUIRED, allowableValues={ "QUANTITY" } )
    public FrequencyTypeEnum frequencyType = null;
    public LocalDate startDate = null;
    public LocalDate lastOccurrenceDate = null;
    public Integer value = null;
}

@Serdeable
@Schema(name = "DeadlineFrequency")
@JsonTypeName("DeadlineFrequency")
class DeadlineFrequency implements RecurringDataFrequencyOneOf {

    @Introspected
    enum FrequencyTypeEnum {
        DEADLINE("DEADLINE");

        private String value;

        FrequencyTypeEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static FrequencyTypeEnum fromValue(String text) {
            for (FrequencyTypeEnum b : FrequencyTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    @Schema(name = "frequencyType", example = "DEADLINE"
            , implementation = String.class, requiredMode = Schema.RequiredMode.NOT_REQUIRED, allowableValues = {"DEADLINE"}
    )
    public FrequencyTypeEnum frequencyType = null;
    public LocalDate startDate = null;
    public LocalDate endDate = null;
    public LocalDate lastOccurrenceDate = null;
}

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeadlineFrequency.class, name = "DeadlineFrequency"),
        @JsonSubTypes.Type(value = QuantityFrequency.class, name = "QuantityFrequency")
})
@Schema(name = "RecurringDataFrequencyOneOf",
        oneOf = {DeadlineFrequency.class, QuantityFrequency.class})
interface RecurringDataFrequencyOneOf {
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        def openApi = Utils.testReference
        openApi
        !openApi.components.schemas.RecurringDataFrequency.properties.oneOf.oneOf
        openApi.components.schemas.RecurringDataFrequency.properties.oneOf.allOf[0].$ref == "#/components/schemas/RecurringDataFrequencyOneOf"
        openApi.components.schemas.RecurringDataFrequencyOneOf.discriminator.propertyName == "type"
        openApi.components.schemas.RecurringDataFrequencyOneOf.discriminator.mapping.QuantityFrequency == "#/components/schemas/QuantityFrequency"
        openApi.components.schemas.RecurringDataFrequencyOneOf.discriminator.mapping.DeadlineFrequency == "#/components/schemas/DeadlineFrequency"
        openApi.components.schemas.RecurringDataFrequencyOneOf.oneOf[0].$ref == "#/components/schemas/DeadlineFrequency"
        openApi.components.schemas.RecurringDataFrequencyOneOf.oneOf[1].$ref == "#/components/schemas/QuantityFrequency"
    }
}
