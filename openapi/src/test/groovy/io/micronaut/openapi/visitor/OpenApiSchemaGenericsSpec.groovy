package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import spock.util.environment.RestoreSystemProperties

class OpenApiSchemaGenericsSpec extends AbstractOpenApiTypeElementSpec {

    void "my annotations with generics"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Controller
class MyController {

    @Get
    public MyDtoImpl doSomeStuff() {
        return new MyDtoImpl();
    }
}

class MyDtoImpl {

    public List<@Nullable String> primitivesList;
    public List<@Nullable ListItem> objectsList;

    public List<@Nullable @Size(max = 10) List<@Nullable String>> nestedPrimitivesList;
    public List<@Nullable @Size(max = 10) List<@Nullable ListItem>> nestedObjectsList;

    public GenObject<@Nullable @Size(min = 10) String> genObjectPrimitive;
    public GenObject<@Size(min = 10) @Nullable String> genObjectPrimitive2;
    public GenObject<@Size(max = 20) @NotBlank String> genObjectPrimitive3;
    public GenObject<String> genObjectPrimitive4;

    public GenObject<@Nullable ListItem> genObjectObj;
    public GenObject<ListItem> genObjectObj2;
    public GenObject<@Nullable GenObject<@Nullable ListItem>> genObjectObj3;

    public GenObject<@Nullable @Size(max = 10) List<@Nullable String>> nestedGenObjectPrimitive;
    public GenObject<@Nullable @Size(max = 10) List<@Nullable GenObject<@Nullable ListItem>>> nestedGenObjectObj;
}

class ListItem {

    public int field;
}

class GenObject<T> {

    public T field;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.get
        Schema schema = openAPI.components.schemas.MyDtoImpl

        expect:
        operation
        operation.responses.size() == 1
        schema
        schema.properties.size() == 13

        schema.properties.primitivesList.type == 'array'
        schema.properties.primitivesList.items
        schema.properties.primitivesList.items.type == 'string'
        schema.properties.primitivesList.items.nullable

        schema.properties.objectsList.type == 'array'
        schema.properties.objectsList.items
        schema.properties.objectsList.items.nullable
        schema.properties.objectsList.items.allOf
        schema.properties.objectsList.items.allOf.size() == 1
        schema.properties.objectsList.items.allOf[0].$ref == '#/components/schemas/ListItem'

        schema.properties.nestedPrimitivesList.type == 'array'
        schema.properties.nestedPrimitivesList.items
        schema.properties.nestedPrimitivesList.items.nullable
        schema.properties.nestedPrimitivesList.items.allOf
        schema.properties.nestedPrimitivesList.items.allOf.size() == 1
        schema.properties.nestedPrimitivesList.items.type == 'array'
        schema.properties.nestedPrimitivesList.items.allOf[0].items
        schema.properties.nestedPrimitivesList.items.allOf[0].items.type == 'string'
        schema.properties.nestedPrimitivesList.items.allOf[0].items.nullable
        schema.properties.nestedPrimitivesList.items.allOf[0].maxItems == 10

        schema.properties.nestedObjectsList.type == 'array'
        schema.properties.nestedObjectsList.items
        schema.properties.nestedObjectsList.items.nullable
        schema.properties.nestedObjectsList.items.allOf
        schema.properties.nestedObjectsList.items.allOf.size() == 1
        schema.properties.nestedObjectsList.items.type == 'array'
        schema.properties.nestedObjectsList.items.allOf[0].items
        schema.properties.nestedObjectsList.items.allOf[0].items.nullable
        schema.properties.nestedObjectsList.items.allOf[0].items.allOf
        schema.properties.nestedObjectsList.items.allOf[0].items.allOf.size() == 1
        schema.properties.nestedObjectsList.items.allOf[0].items.allOf[0].$ref == '#/components/schemas/ListItem'
        schema.properties.nestedObjectsList.items.allOf[0].maxItems == 10

        schema.properties.genObjectPrimitive.$ref == '#/components/schemas/GenObject_Size_min_10_NullableString_'
        schema.properties.genObjectPrimitive2.$ref == '#/components/schemas/GenObject_Size_min_10_NullableString_'
        schema.properties.genObjectPrimitive3.$ref == '#/components/schemas/GenObject_Size_max_20_NotBlankString_'
        schema.properties.genObjectPrimitive4.$ref == '#/components/schemas/GenObject_String_'

        def subSchema1 = openAPI.components.schemas.GenObject_Size_max_20_NotBlankString_
        subSchema1.properties.field.maxLength == 20
        subSchema1.properties.field.minLength == 1
        subSchema1.properties.field.type == 'string'

        def subSchema2 = openAPI.components.schemas.GenObject_Size_min_10_NullableString_
        subSchema2.properties.field.nullable
        subSchema2.properties.field.minLength == 10
        subSchema2.properties.field.type == 'string'

        def subSchema3 = openAPI.components.schemas.GenObject_String_
        subSchema3.properties.field.type == 'string'

        schema.properties.genObjectObj.nullable
        schema.properties.genObjectObj.allOf
        schema.properties.genObjectObj.allOf.size() == 1
        schema.properties.genObjectObj.allOf[0].$ref == '#/components/schemas/GenObject_ListItem_'

        !schema.properties.genObjectObj2.allOf
        schema.properties.genObjectObj2.$ref == '#/components/schemas/GenObject_ListItem_'

        schema.properties.genObjectObj3.nullable
        schema.properties.genObjectObj3.allOf
        schema.properties.genObjectObj3.allOf.size() == 1
        schema.properties.genObjectObj3.allOf[0].$ref == '#/components/schemas/GenObject_GenObject_ListItem__'

        schema.properties.nestedGenObjectPrimitive.nullable
        schema.properties.nestedGenObjectPrimitive.allOf
        schema.properties.nestedGenObjectPrimitive.allOf.size() == 2
        schema.properties.nestedGenObjectPrimitive.allOf[0].$ref == '#/components/schemas/GenObject_List_NullableString__'
        schema.properties.nestedGenObjectPrimitive.allOf[1].maxItems == 10

        schema.properties.nestedGenObjectObj.nullable
        schema.properties.nestedGenObjectObj.allOf
        schema.properties.nestedGenObjectObj.allOf.size() == 2
        schema.properties.nestedGenObjectObj.allOf[0].$ref == '#/components/schemas/GenObject_List_GenObject_ListItem___'
        schema.properties.nestedGenObjectObj.allOf[1].maxItems == 10
    }

    void "Issue #279 - test parse OpenAPI with generics in interface"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.annotation.*;
import java.util.List;
import java.time.Instant;

@Controller("/")
class MyController {

    @Get("/")
    public MyDtoImpl doSomeStuff() {
        return new MyDtoImpl();
    }
}

interface MyDto<ID> {

    ID getId();
    void setId(ID id);

    Instant getVersion();
    void setVersion(Instant version);
}

class MyDtoImpl implements MyDto<Long> {

    private Long id;
    private Instant version;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Instant getVersion() {
        return version;
    }

    @Override
    public void setVersion(Instant version) {
        this.version = version;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.get

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['MyDtoImpl']
        openAPI.components.schemas['MyDtoImpl'].properties.size() == 2
        openAPI.components.schemas['MyDtoImpl'].properties['id'].type == 'integer'
        openAPI.components.schemas['MyDtoImpl'].properties['id'].format == 'int64'
        openAPI.components.schemas['MyDtoImpl'].properties['version'].type == 'string'
        openAPI.components.schemas['MyDtoImpl'].properties['version'].format == 'date-time'
    }

    void "test parse OpenAPI with recursive generics"() {
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
    public Pet updatePet(Pet pet) {
        return null;
    }
}

class Pet implements java.util.function.Consumer<Pet> {
    private PetType type;

    public void setType(PetType type) {
        this.type = type;
    }

    /**
     * The age
     */
    public PetType getType() {
        return this.type;
    }

    @Override
    public void accept(Pet pet) {}
}

enum PetType {
    DOG, CAT;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['Pet']
    }

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
    public Response<Pet> updatePet(Pet pet) {
        return null;
    }
}

class Pet {
    @jakarta.validation.constraints.Min(18)
    private int age;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    public int getAge() {
        return age;
    }
}

class Response<T> {
    T r;
    public T getResult() {
        return r;
    };
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['Pet'].properties['age'].type == 'integer'
        openAPI.components.schemas['Response_Pet_'].properties['result'].$ref == '#/components/schemas/Pet'
    }

    void "test parse the OpenAPI with response that contains nested generic types"() {
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
    public Response<List<Pet>> updatePet(@Body Response<List<Pet>> pet) {
        return null;
    }
}

class Pet {
    @jakarta.validation.constraints.Min(18)
    private int age;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    public int getAge() {
        return age;
    }
}

class Response<T> {
    T r;
    public T getResult() {
        return r;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['Pet'].properties['age'].type == 'integer'
        openAPI.components.schemas['Response_List_Pet__'].properties['result'].type == 'array'
        openAPI.components.schemas['Response_List_Pet__'].properties['result'].items.$ref == '#/components/schemas/Pet'
    }

    void "test parse the OpenAPI with response that contains generic types and custom schema name"() {
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
    @Schema(name="ResponseOfPet")
    public Response<Pet> updatePet(@Body @Schema(name="RequestOfPet") Response<Pet> pet) {
        return null;
    }
}

class Pet {
    @jakarta.validation.constraints.Min(18)
    private int age;

    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    public int getAge() {
        return age;
    }
}

class Response<T> {
    T r;
    public T getResult() {
        return r;
    };
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        operation.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/ResponseOfPet'
        operation.requestBody.content['application/json'].schema.$ref == '#/components/schemas/RequestOfPet'
        openAPI.components.schemas['Pet'].properties['age'].type == 'integer'
        openAPI.components.schemas['ResponseOfPet'].properties['result'].$ref == '#/components/schemas/Pet'
        openAPI.components.schemas['RequestOfPet'].properties['result'].$ref == '#/components/schemas/Pet'
    }

    void "test schema with generic types - Issue #259"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import static java.math.RoundingMode.HALF_UP;

import io.micronaut.http.annotation.*;

@Controller
class TimeController {

    /**
     * Time of 10 seconds
     * @return Time of 10 seconds
     */
    @Get("/time")
    Time getTime() {
        return Time.of(BigDecimal.TEN, TimeUnit.Second);
    }
}

enum TimeUnit implements Unit {
    Millisecond(BigDecimal.ONE.divide(BigDecimal.valueOf(1000), MATH_CONTEXT), "ms"),
    Second(BigDecimal.ONE, "s"),
    Minute(BigDecimal.valueOf(60), Second, "m"),
    Hour(BigDecimal.valueOf(60), Minute, "h"),
    Day(BigDecimal.valueOf(24), Hour, "d"),
    Week(BigDecimal.valueOf(7), Day, "w");

    private final BigDecimal ratio;
    private final String suffix;

    TimeUnit(BigDecimal ratio, String suffix) {
        this.ratio = ratio;
        this.suffix = suffix;
    }

    TimeUnit(BigDecimal factor, TimeUnit base, String suffix) {
        this.ratio = factor.multiply(base.ratio);
        this.suffix = suffix;
    }

    @Override
    public BigDecimal ratio() {
        return ratio;
    }

    @Override
    public String suffix() {
        return suffix;
    }

}

interface Unit {

    MathContext MATH_CONTEXT = new MathContext(16, HALF_UP);

    String name();

    BigDecimal ratio();

    String suffix();
}

class Time extends Quantity<Time, TimeUnit> {

    private Time(BigDecimal amount, TimeUnit unit) {
        super(amount, unit);
    }

    public static Time of(BigDecimal amount, TimeUnit unit) {
        return new Time(amount, unit);
    }

    @Override
    public BigDecimal getAmount() {
        return super.getAmount();
    }

    @Override
    public TimeUnit getUnit() {
        return super.getUnit();
    }

    @Override
    public void setAmount(BigDecimal amount) {
        super.setAmount(amount);
    }

    @Override
    public void setUnit(TimeUnit unit) {
        super.setUnit(unit);
    }

}

class Quantity<Q extends Quantity, U extends Unit> implements Serializable {

    private static final long serialVersionUID = -9000608810227353935L;

    private final BigDecimal amount;
    private final U unit;

    Quantity(BigDecimal amount, U unit) {
        this.amount = amount;
        this.unit = unit;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public U getUnit() {
        return unit;
    }

    public void setUnit(U unit) {
        throw new UnsupportedOperationException("Quantities can't change");
    }

    public void setAmount(BigDecimal amount) {
        throw new UnsupportedOperationException("Quantities can't change");
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/time")?.get

        expect:
        operation
        operation.responses.size() == 1
        operation.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/Time'
        openAPI.components.schemas['Quantity_Time.TimeUnit_'].type == 'object'
        openAPI.components.schemas['Quantity_Time.TimeUnit_'].properties['amount'].type == 'number'
        openAPI.components.schemas['Quantity_Time.TimeUnit_'].properties['unit'].$ref == '#/components/schemas/TimeUnit'
        openAPI.components.schemas['TimeUnit'].type == 'string'
        openAPI.components.schemas['TimeUnit'].enum.size() == 6
        openAPI.components.schemas['TimeUnit'].enum[0] == 'Millisecond'
        openAPI.components.schemas['TimeUnit'].enum[1] == 'Second'
        openAPI.components.schemas['TimeUnit'].enum[2] == 'Minute'
        openAPI.components.schemas['TimeUnit'].enum[3] == 'Hour'
        openAPI.components.schemas['TimeUnit'].enum[4] == 'Day'
        openAPI.components.schemas['TimeUnit'].enum[5] == 'Week'
        openAPI.components.schemas['Time'].$ref == '#/components/schemas/Quantity_Time.TimeUnit_'
    }

    void "test schema with generic wildcard or placeholder"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.Collection;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

@Controller
class CommonController {

    @Get("/get1")
    public <T extends Channel> String index1(@Nullable @QueryValue T[] channels) {
        return null;
    }

    @Get("/get2")
    public String index2(@Nullable @QueryValue Collection<? extends Channel> channels) {
        return null;
    }

    @Get("/get3")
    public String index3(@Nullable @QueryValue Collection<@Nullable ? extends Channel> channels) {
        return null;
    }

    @Introspected
    enum Channel {
        SYSTEM1,
        SYSTEM2
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation get1 = openAPI.paths?.get("/get1")?.get
        Operation get2 = openAPI.paths?.get("/get2")?.get
        Operation get3 = openAPI.paths?.get("/get3")?.get

        expect:
        get1
        get1.parameters[0].name == 'channels'
        get1.parameters[0].in == 'query'
        get1.parameters[0].schema
        get1.parameters[0].schema.type == 'array'
        get1.parameters[0].schema.nullable
        get1.parameters[0].schema.items.$ref == '#/components/schemas/CommonController.Channel'

        get2
        get2.parameters[0].name == 'channels'
        get2.parameters[0].in == 'query'
        get2.parameters[0].schema
        get2.parameters[0].schema.type == 'array'
        get2.parameters[0].schema.nullable
        get2.parameters[0].schema.items.$ref == '#/components/schemas/CommonController.Channel'

        get3
        get3.parameters[0].name == 'channels'
        get3.parameters[0].in == 'query'
        get3.parameters[0].schema
        get3.parameters[0].schema.type == 'array'
        get3.parameters[0].schema.nullable
        get3.parameters[0].schema.items.nullable
        get3.parameters[0].schema.items.allOf
        get3.parameters[0].schema.items.allOf[0].$ref == '#/components/schemas/CommonController.Channel'
    }

    void "test schema with public fields type arguments"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.Map;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
class CommonController {

    @Get("/get1")
    public ListElement1<String> list1() {
        return null;
    }

    @Get("/get2")
    public ListElement2<String> list2() {
        return null;
    }

    @Get("/get3")
    public ListElement3<String> list3() {
        return null;
    }
}

class ListElement1<T> {

    private T typeValue;
    private Iterable<T> iterableValues;
    private T[] arrayValues;

    public T getTypeValue() {
        return typeValue;
    }

    public void setTypeValue(T typeValue) {
        this.typeValue = typeValue;
    }

    public Iterable<T> getIterableValues() {
        return iterableValues;
    }

    public void setIterableValues(Iterable<T> iterableValues) {
        this.iterableValues = iterableValues;
    }

    public T[] getArrayValues() {
        return arrayValues;
    }

    public void setArrayValues(T[] arrayValues) {
        this.arrayValues = arrayValues;
    }
}

record ListElement2<T>(
    T typeValue,
    Iterable<T> iterableValues,
    T[] arrayValues
) {}

class ListElement3<T> {

    public T typeValue;
    public Iterable<T> iterableValues;
    public T[] arrayValues;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        def schema1 = openAPI.components.schemas.'ListElement1_String_'
        def schema2 = openAPI.components.schemas.'ListElement2_String_'
        def schema3 = openAPI.components.schemas.'ListElement3_String_'

        expect:
        schema1.type == 'object'
        schema1.properties.typeValue.type == 'string'
        schema1.properties.arrayValues.type == 'array'
        schema1.properties.arrayValues.items.type == 'string'
        schema1.properties.iterableValues.type == 'array'
        schema1.properties.iterableValues.items.type == 'string'

        schema2.type == 'object'
        schema2.properties.typeValue.type == 'string'
        schema2.properties.arrayValues.type == 'array'
        schema2.properties.arrayValues.items.type == 'string'
        schema2.properties.iterableValues.type == 'array'
        schema2.properties.iterableValues.items.type == 'string'

        schema3.type == 'object'
        schema3.properties.typeValue.type == 'string'
        schema3.properties.arrayValues.type == 'array'
        schema3.properties.arrayValues.items.type == 'string'
        schema3.properties.iterableValues.type == 'array'
        schema3.properties.iterableValues.items.type == 'string'
    }

    @RestoreSystemProperties
    void "my annotations with generics with empty separator"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_EMPTY, "true")

        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Controller
class MyController {

    @Get
    public MyDtoImpl doSomeStuff() {
        return new MyDtoImpl();
    }
}

class MyDtoImpl {

    public List<@Nullable String> primitivesList;
    public List<@Nullable ListItem> objectsList;

    public List<@Nullable @Size(max = 10) List<@Nullable String>> nestedPrimitivesList;
    public List<@Nullable @Size(max = 10) List<@Nullable ListItem>> nestedObjectsList;

    public GenObject<@Nullable @Size(min = 10) String> genObjectPrimitive;
    public GenObject<@Size(min = 10) @Nullable String> genObjectPrimitive2;
    public GenObject<@Size(max = 20) @NotBlank String> genObjectPrimitive3;
    public GenObject<String> genObjectPrimitive4;

    public GenObject<@Nullable ListItem> genObjectObj;
    public GenObject<ListItem> genObjectObj2;
    public GenObject<@Nullable GenObject<@Nullable ListItem>> genObjectObj3;

    public GenObject<@Nullable @Size(max = 10) List<@Nullable String>> nestedGenObjectPrimitive;
    public GenObject<@Nullable @Size(max = 10) List<@Nullable GenObject<@Nullable ListItem>>> nestedGenObjectObj;
}

class ListItem {

    public int field;
}

class GenObject<T> {

    public T field;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.get
        Schema schema = openAPI.components.schemas.MyDtoImpl

        then:
        operation
        operation.responses.size() == 1
        schema
        schema.properties.size() == 13

        schema.properties.primitivesList.type == 'array'
        schema.properties.primitivesList.items
        schema.properties.primitivesList.items.type == 'string'
        schema.properties.primitivesList.items.nullable

        schema.properties.objectsList.type == 'array'
        schema.properties.objectsList.items
        schema.properties.objectsList.items.nullable
        schema.properties.objectsList.items.allOf
        schema.properties.objectsList.items.allOf.size() == 1
        schema.properties.objectsList.items.allOf[0].$ref == '#/components/schemas/ListItem'

        schema.properties.nestedPrimitivesList.type == 'array'
        schema.properties.nestedPrimitivesList.items
        schema.properties.nestedPrimitivesList.items.nullable
        schema.properties.nestedPrimitivesList.items.allOf
        schema.properties.nestedPrimitivesList.items.allOf.size() == 1
        schema.properties.nestedPrimitivesList.items.type == 'array'
        schema.properties.nestedPrimitivesList.items.allOf[0].items
        schema.properties.nestedPrimitivesList.items.allOf[0].items.type == 'string'
        schema.properties.nestedPrimitivesList.items.allOf[0].items.nullable
        schema.properties.nestedPrimitivesList.items.allOf[0].maxItems == 10

        schema.properties.nestedObjectsList.type == 'array'
        schema.properties.nestedObjectsList.items
        schema.properties.nestedObjectsList.items.nullable
        schema.properties.nestedObjectsList.items.allOf
        schema.properties.nestedObjectsList.items.allOf.size() == 1
        schema.properties.nestedObjectsList.items.type == 'array'
        schema.properties.nestedObjectsList.items.allOf[0].items
        schema.properties.nestedObjectsList.items.allOf[0].items.nullable
        schema.properties.nestedObjectsList.items.allOf[0].items.allOf
        schema.properties.nestedObjectsList.items.allOf[0].items.allOf.size() == 1
        schema.properties.nestedObjectsList.items.allOf[0].items.allOf[0].$ref == '#/components/schemas/ListItem'
        schema.properties.nestedObjectsList.items.allOf[0].maxItems == 10

        schema.properties.genObjectPrimitive.$ref == '#/components/schemas/GenObjectSizemin10NullableString'
        schema.properties.genObjectPrimitive2.$ref == '#/components/schemas/GenObjectSizemin10NullableString'
        schema.properties.genObjectPrimitive3.$ref == '#/components/schemas/GenObjectSizemax20NotBlankString'
        schema.properties.genObjectPrimitive4.$ref == '#/components/schemas/GenObjectString'

        def subSchema1 = openAPI.components.schemas.GenObjectSizemax20NotBlankString
        subSchema1.properties.field.maxLength == 20
        subSchema1.properties.field.minLength == 1
        subSchema1.properties.field.type == 'string'

        def subSchema2 = openAPI.components.schemas.GenObjectSizemin10NullableString
        subSchema2.properties.field.nullable
        subSchema2.properties.field.minLength == 10
        subSchema2.properties.field.type == 'string'

        def subSchema3 = openAPI.components.schemas.GenObjectString
        subSchema3.properties.field.type == 'string'

        schema.properties.genObjectObj.nullable
        schema.properties.genObjectObj.allOf
        schema.properties.genObjectObj.allOf.size() == 1
        schema.properties.genObjectObj.allOf[0].$ref == '#/components/schemas/GenObjectListItem'

        !schema.properties.genObjectObj2.allOf
        schema.properties.genObjectObj2.$ref == '#/components/schemas/GenObjectListItem'

        schema.properties.genObjectObj3.nullable
        schema.properties.genObjectObj3.allOf
        schema.properties.genObjectObj3.allOf.size() == 1
        schema.properties.genObjectObj3.allOf[0].$ref == '#/components/schemas/GenObjectGenObjectListItem'

        schema.properties.nestedGenObjectPrimitive.nullable
        schema.properties.nestedGenObjectPrimitive.allOf
        schema.properties.nestedGenObjectPrimitive.allOf.size() == 2
        schema.properties.nestedGenObjectPrimitive.allOf[0].$ref == '#/components/schemas/GenObjectListNullableString'
        schema.properties.nestedGenObjectPrimitive.allOf[1].maxItems == 10

        schema.properties.nestedGenObjectObj.nullable
        schema.properties.nestedGenObjectObj.allOf
        schema.properties.nestedGenObjectObj.allOf.size() == 2
        schema.properties.nestedGenObjectObj.allOf[0].$ref == '#/components/schemas/GenObjectListGenObjectListItem'
        schema.properties.nestedGenObjectObj.allOf[1].maxItems == 10
    }

    @RestoreSystemProperties
    void "my annotations with generics with custom separators"() {
        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_GENERIC, "<<<")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_SCHEMA_NAME_SEPARATOR_INNER_CLASS, "&&&")

        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.PubGenObject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Controller
class MyController {

    @Get
    public MyDtoImpl doSomeStuff() {
        return new MyDtoImpl();
    }
}

class MyDtoImpl {

    public List<@Nullable String> primitivesList;
    public List<@Nullable ListItem> objectsList;

    public List<@Nullable @Size(max = 10) List<@Nullable String>> nestedPrimitivesList;
    public List<@Nullable @Size(max = 10) List<@Nullable ListItem>> nestedObjectsList;

    public GenObject<@Nullable @Size(min = 10) String> genObjectPrimitive;
    public GenObject<@Size(min = 10) @Nullable String> genObjectPrimitive2;
    public GenObject<@Size(max = 20) @NotBlank String> genObjectPrimitive3;
    public GenObject<String> genObjectPrimitive4;

    public GenObject<@Nullable ListItem> genObjectObj;
    public GenObject<ListItem> genObjectObj2;
    public GenObject<@Nullable GenObject<@Nullable ListItem>> genObjectObj3;

    public GenObject<@Nullable @Size(max = 10) List<@Nullable String>> nestedGenObjectPrimitive;
    public GenObject<@Nullable @Size(max = 10) List<@Nullable GenObject<@Nullable ListItem>>> nestedGenObjectObj;

    public GenObject<@Nullable @Size(max = 10) List<@Nullable PubGenObject<PubGenObject.ListInnerItem>>> withInnerClass;
}

class ListItem {

    public int field;
}

class GenObject<T> {

    public T field;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.get
        Schema schema = openAPI.components.schemas.MyDtoImpl

        then:
        operation
        operation.responses.size() == 1
        schema
        schema.properties.size() == 14

        schema.properties.primitivesList.type == 'array'
        schema.properties.primitivesList.items
        schema.properties.primitivesList.items.type == 'string'
        schema.properties.primitivesList.items.nullable

        schema.properties.objectsList.type == 'array'
        schema.properties.objectsList.items
        schema.properties.objectsList.items.nullable
        schema.properties.objectsList.items.allOf
        schema.properties.objectsList.items.allOf.size() == 1
        schema.properties.objectsList.items.allOf[0].$ref == '#/components/schemas/ListItem'

        schema.properties.nestedPrimitivesList.type == 'array'
        schema.properties.nestedPrimitivesList.items
        schema.properties.nestedPrimitivesList.items.nullable
        schema.properties.nestedPrimitivesList.items.allOf
        schema.properties.nestedPrimitivesList.items.allOf.size() == 1
        schema.properties.nestedPrimitivesList.items.type == 'array'
        schema.properties.nestedPrimitivesList.items.allOf[0].items
        schema.properties.nestedPrimitivesList.items.allOf[0].items.type == 'string'
        schema.properties.nestedPrimitivesList.items.allOf[0].items.nullable
        schema.properties.nestedPrimitivesList.items.allOf[0].maxItems == 10

        schema.properties.nestedObjectsList.type == 'array'
        schema.properties.nestedObjectsList.items
        schema.properties.nestedObjectsList.items.nullable
        schema.properties.nestedObjectsList.items.allOf
        schema.properties.nestedObjectsList.items.allOf.size() == 1
        schema.properties.nestedObjectsList.items.type == 'array'
        schema.properties.nestedObjectsList.items.allOf[0].items
        schema.properties.nestedObjectsList.items.allOf[0].items.nullable
        schema.properties.nestedObjectsList.items.allOf[0].items.allOf
        schema.properties.nestedObjectsList.items.allOf[0].items.allOf.size() == 1
        schema.properties.nestedObjectsList.items.allOf[0].items.allOf[0].$ref == '#/components/schemas/ListItem'
        schema.properties.nestedObjectsList.items.allOf[0].maxItems == 10

        schema.properties.genObjectPrimitive.$ref == '#/components/schemas/GenObject<<<Size<<<min<<<10<<<NullableString<<<'
        schema.properties.genObjectPrimitive2.$ref == '#/components/schemas/GenObject<<<Size<<<min<<<10<<<NullableString<<<'
        schema.properties.genObjectPrimitive3.$ref == '#/components/schemas/GenObject<<<Size<<<max<<<20<<<NotBlankString<<<'
        schema.properties.genObjectPrimitive4.$ref == '#/components/schemas/GenObject<<<String<<<'

        def subSchema1 = openAPI.components.schemas.'GenObject<<<Size<<<max<<<20<<<NotBlankString<<<'
        subSchema1.properties.field.maxLength == 20
        subSchema1.properties.field.minLength == 1
        subSchema1.properties.field.type == 'string'

        def subSchema2 = openAPI.components.schemas.'GenObject<<<Size<<<min<<<10<<<NullableString<<<'
        subSchema2.properties.field.nullable
        subSchema2.properties.field.minLength == 10
        subSchema2.properties.field.type == 'string'

        def subSchema3 = openAPI.components.schemas.'GenObject<<<String<<<'
        subSchema3.properties.field.type == 'string'

        schema.properties.genObjectObj.nullable
        schema.properties.genObjectObj.allOf
        schema.properties.genObjectObj.allOf.size() == 1
        schema.properties.genObjectObj.allOf[0].$ref == '#/components/schemas/GenObject<<<ListItem<<<'

        !schema.properties.genObjectObj2.allOf
        schema.properties.genObjectObj2.$ref == '#/components/schemas/GenObject<<<ListItem<<<'

        schema.properties.genObjectObj3.nullable
        schema.properties.genObjectObj3.allOf
        schema.properties.genObjectObj3.allOf.size() == 1
        schema.properties.genObjectObj3.allOf[0].$ref == '#/components/schemas/GenObject<<<GenObject<<<ListItem<<<<<<'

        schema.properties.nestedGenObjectPrimitive.nullable
        schema.properties.nestedGenObjectPrimitive.allOf
        schema.properties.nestedGenObjectPrimitive.allOf.size() == 2
        schema.properties.nestedGenObjectPrimitive.allOf[0].$ref == '#/components/schemas/GenObject<<<List<<<NullableString<<<<<<'
        schema.properties.nestedGenObjectPrimitive.allOf[1].maxItems == 10

        schema.properties.nestedGenObjectObj.nullable
        schema.properties.nestedGenObjectObj.allOf
        schema.properties.nestedGenObjectObj.allOf.size() == 2
        schema.properties.nestedGenObjectObj.allOf[0].$ref == '#/components/schemas/GenObject<<<List<<<GenObject<<<ListItem<<<<<<<<<'
        schema.properties.nestedGenObjectObj.allOf[1].maxItems == 10

        schema.properties.withInnerClass.nullable
        schema.properties.withInnerClass.allOf
        schema.properties.withInnerClass.allOf.size() == 2
        schema.properties.withInnerClass.allOf[0].$ref == "#/components/schemas/GenObject<<<List<<<PubGenObject<<<PubGenObject&&&ListInnerItem<<<<<<<<<"
        schema.properties.withInnerClass.allOf[1].maxItems == 10
    }
}
