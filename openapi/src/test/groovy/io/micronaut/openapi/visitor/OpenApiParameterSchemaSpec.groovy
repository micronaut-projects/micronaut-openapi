package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiParameterSchemaSpec extends AbstractOpenApiTypeElementSpec {

    void "test basic java classes"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.lang.Number;

@Controller
class OpenApiController {

    @Get("/Period")
    public Period getPeriod(@QueryValue Period param) {
        return null;
    }

    @Get("/Duration")
    public Duration getDuration(@QueryValue Duration param) {
        return null;
    }

    @Get("/LocalDate")
    public LocalDate getLocalDate(@QueryValue LocalDate param) {
        return null;
    }

    @Get("/Instant")
    public Instant getInstant(@QueryValue Instant param) {
        return null;
    }

    @Get("/ZonedDateTime")
    public ZonedDateTime getZonedDateTime(@QueryValue ZonedDateTime param) {
        return null;
    }

    @Get("/Date")
    public Date getDate(@QueryValue Date param) {
        return null;
    }

    @Get("/OffsetDateTime")
    public OffsetDateTime getOffsetDateTime(@QueryValue OffsetDateTime param) {
        return null;
    }

    @Get("/OffsetTime")
    public OffsetTime getOffsetTime(@QueryValue OffsetTime param) {
        return null;
    }

    @Get("/LocalTime")
    public LocalTime getLocalTime(@QueryValue LocalTime param) {
        return null;
    }

    @Get("/YearMonth")
    public YearMonth getYearMonth(@QueryValue YearMonth param) {
        return null;
    }

    @Get("/Year")
    public Year getYear(@QueryValue Year param) {
        return null;
    }

    @Get("/MonthDay")
    public MonthDay getMonthDay(@QueryValue MonthDay param) {
        return null;
    }

    @Get("/ZoneId")
    public ZoneId getZoneId(@QueryValue ZoneId param) {
        return null;
    }

    @Get("/ZoneOffset")
    public ZoneOffset getZoneOffset(@QueryValue ZoneOffset param) {
        return null;
    }

    @Get("/UUID")
    public UUID getUUID(@QueryValue UUID param) {
        return null;
    }

    @Get("/BigDecimal")
    public BigDecimal getBigDecimal(@QueryValue BigDecimal param) {
        return null;
    }

    @Get("/BigInteger")
    public BigInteger getBigInteger(@QueryValue BigInteger param) {
        return null;
    }

    @Get("/URL")
    public URL getURL(@QueryValue URL param) {
        return null;
    }

    @Get("/URI")
    public URI getURI(@QueryValue URI param) {
        return null;
    }

    @Get("/TimeZone")
    public TimeZone getTimeZone(@QueryValue TimeZone param) {
        return null;
    }

    @Get("/Charset")
    public Charset getCharset(@QueryValue Charset param) {
        return null;
    }

    @Get("/Locale")
    public Locale getLocale(@QueryValue Locale param) {
        return null;
    }

    @Get("/Number")
    public Number getNumber(@QueryValue Number param) {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        def paths = openAPI.paths

        then:
        paths

        paths."/BigDecimal".get.parameters[0].schema.type == 'number'
        paths."/BigDecimal".get.responses."200".content."application/json".schema.type == 'number'

        paths."/BigInteger".get.parameters[0].schema.type == 'integer'
        paths."/BigInteger".get.responses."200".content."application/json".schema.type == 'integer'

        paths."/Charset".get.parameters[0].schema.type == 'string'
        paths."/Charset".get.responses."200".content."application/json".schema.type == 'string'

        paths."/Date".get.parameters[0].schema.type == 'string'
        paths."/Date".get.parameters[0].schema.format == 'date-time'
        paths."/Date".get.responses."200".content."application/json".schema.type == 'string'
        paths."/Date".get.responses."200".content."application/json".schema.format == 'date-time'

        paths."/Duration".get.parameters[0].schema.type == 'string'
        paths."/Duration".get.responses."200".content."application/json".schema.type == 'string'

        paths."/Instant".get.parameters[0].schema.type == 'string'
        paths."/Instant".get.parameters[0].schema.format == 'date-time'
        paths."/Instant".get.responses."200".content."application/json".schema.type == 'string'
        paths."/Instant".get.responses."200".content."application/json".schema.format == 'date-time'

        paths."/LocalDate".get.parameters[0].schema.type == 'string'
        paths."/LocalDate".get.parameters[0].schema.format == 'date'
        paths."/LocalDate".get.responses."200".content."application/json".schema.type == 'string'
        paths."/LocalDate".get.responses."200".content."application/json".schema.format == 'date'

        paths."/LocalTime".get.parameters[0].schema.type == 'string'
        paths."/LocalTime".get.parameters[0].schema.format == 'partial-time'
        paths."/LocalTime".get.responses."200".content."application/json".schema.type == 'string'
        paths."/LocalTime".get.responses."200".content."application/json".schema.format == 'partial-time'

        paths."/Locale".get.parameters[0].schema.type == 'string'
        paths."/Locale".get.responses."200".content."application/json".schema.type == 'string'

        paths."/MonthDay".get.parameters[0].schema.type == 'string'
        paths."/MonthDay".get.responses."200".content."application/json".schema.type == 'string'

        paths."/Number".get.parameters[0].schema.type == 'number'
        paths."/Number".get.responses."200".content."application/json".schema.type == 'number'

        paths."/OffsetDateTime".get.parameters[0].schema.type == 'string'
        paths."/OffsetDateTime".get.parameters[0].schema.format == 'date-time'
        paths."/OffsetDateTime".get.responses."200".content."application/json".schema.type == 'string'
        paths."/OffsetDateTime".get.responses."200".content."application/json".schema.format == 'date-time'

        paths."/OffsetTime".get.parameters[0].schema.type == 'string'
        paths."/OffsetTime".get.responses."200".content."application/json".schema.type == 'string'

        paths."/Period".get.parameters[0].schema.type == 'string'
        paths."/Period".get.responses."200".content."application/json".schema.type == 'string'

        paths."/TimeZone".get.parameters[0].schema.type == 'string'
        paths."/TimeZone".get.responses."200".content."application/json".schema.type == 'string'

        paths."/URI".get.parameters[0].schema.type == 'string'
        paths."/URI".get.parameters[0].schema.format == 'uri'
        paths."/URI".get.responses."200".content."application/json".schema.type == 'string'
        paths."/URI".get.responses."200".content."application/json".schema.format == 'uri'

        paths."/URL".get.parameters[0].schema.type == 'string'
        paths."/URL".get.parameters[0].schema.format == 'url'
        paths."/URL".get.responses."200".content."application/json".schema.type == 'string'
        paths."/URL".get.responses."200".content."application/json".schema.format == 'url'

        paths."/UUID".get.parameters[0].schema.type == 'string'
        paths."/UUID".get.parameters[0].schema.format == 'uuid'
        paths."/UUID".get.responses."200".content."application/json".schema.type == 'string'
        paths."/UUID".get.responses."200".content."application/json".schema.format == 'uuid'

        paths."/Year".get.parameters[0].schema.type == 'string'
        paths."/Year".get.responses."200".content."application/json".schema.type == 'string'

        paths."/YearMonth".get.parameters[0].schema.type == 'string'
        paths."/YearMonth".get.responses."200".content."application/json".schema.type == 'string'

        paths."/ZoneId".get.parameters[0].schema.type == 'string'
        paths."/ZoneId".get.responses."200".content."application/json".schema.type == 'string'

        paths."/ZoneOffset".get.parameters[0].schema.type == 'string'
        paths."/ZoneOffset".get.responses."200".content."application/json".schema.type == 'string'

        paths."/ZonedDateTime".get.parameters[0].schema.type == 'string'
        paths."/ZonedDateTime".get.parameters[0].schema.format == 'date-time'
        paths."/ZonedDateTime".get.responses."200".content."application/json".schema.type == 'string'
        paths."/ZonedDateTime".get.responses."200".content."application/json".schema.format == 'date-time'
    }

    void "test parameter with schema"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Period;
import java.util.Optional;

@Controller("/path")
class OpenApiController {

    @Patch
    public HttpResponse<String> processSync0(@QueryValue Optional<Period> period) {
        return HttpResponse.ok();
    }

    @Get
    public HttpResponse<String> processSync(
            @Parameter(schema = @Schema(implementation = String.class)) @QueryValue Optional<Period> period) {
        return HttpResponse.ok();
    }

    @Post
    public HttpResponse<String> processSync2(
            @Parameter(schema = @Schema(minLength = 10, maxLength = 20)) @QueryValue Optional<Period> period) {
        return HttpResponse.ok();
    }

    @Put
    public HttpResponse<String> processSync3(
            @Parameter(schema = @Schema(ref = "#/components/schemas/MyParamSchema")) @QueryValue Optional<Period> period) {
        return HttpResponse.ok();
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operationPatch = openAPI.paths."/path".patch
        Operation operation = openAPI.paths."/path".get
        Operation operationPost = openAPI.paths."/path".post
        Operation operationPut = openAPI.paths."/path".put

        then:
        operationPatch
        operationPatch.operationId == "processSync0"
        operationPatch.parameters
        operationPatch.parameters[0].name == "period"
        operationPatch.parameters[0].in == "query"
        operationPatch.parameters[0].schema
        operationPatch.parameters[0].schema.type == "string"

        operation
        operation.operationId == "processSync"
        operation.parameters
        operation.parameters[0].name == "period"
        operation.parameters[0].in == "query"
        operation.parameters[0].schema
        operation.parameters[0].schema.type == "string"

        operationPost.parameters[0].schema
        operationPost.parameters[0].schema.minLength == 10
        operationPost.parameters[0].schema.maxLength == 20

        operationPut.parameters[0].schema.allOf.get(0).get$ref() == "#/components/schemas/MyParamSchema"
    }

    void "test parameter with schema with attributes"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller("/path")
class OpenApiController {

    @Patch
    public HttpResponse<String> processSync4(@Parameter(schema = @Schema(type = "string", format = "uuid")) @QueryValue String param) {
        return HttpResponse.ok();
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operationPatch = openAPI.paths."/path".patch

        then:
        operationPatch.parameters[0].name == 'param'
        operationPatch.parameters[0].schema.type == 'string'
        operationPatch.parameters[0].schema.format == 'uuid'
    }

    void "test parameters schema with annotation"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.inject.Singleton;

@Controller
class HelloController {

    @Operation(summary = "Test schema")
    @Post(value = "/test")
    public String test(
            @Parameter(description = "test request parameter", schema = @Schema(implementation = String.class, allowableValues = {"foo", "bar"})) @QueryValue("test")
            String test) {
        return "test";
    }

    @Operation(summary = "Test2 schema")
    @Post(value = "/test2")
    public String test2(
        @Parameter(description = "test request parameter", schema = @Schema(allowableValues = {"1", "2"})) @QueryValue("test")
        Integer test) {
        return "test2";
    }
}

@Singleton
class MyBean {}
''')
        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference

        then: "the state is correct"
        openAPI != null

        when:
        Operation testOp = openAPI.paths.get("/test").post
        Operation test2Op = openAPI.paths.get("/test2").post

        then:
        testOp
        testOp.parameters
        testOp.parameters[0].name == 'test'
        testOp.parameters[0].in == 'query'
        testOp.parameters[0].description == 'test request parameter'
        testOp.parameters[0].required
        testOp.parameters[0].schema
        testOp.parameters[0].schema.type == 'string'
        testOp.parameters[0].schema.enum
        testOp.parameters[0].schema.enum[0] == 'foo'
        testOp.parameters[0].schema.enum[1] == 'bar'

        test2Op
        test2Op.parameters
        test2Op.parameters[0].name == 'test'
        test2Op.parameters[0].in == 'query'
        test2Op.parameters[0].description == 'test request parameter'
        test2Op.parameters[0].required
        test2Op.parameters[0].schema
        test2Op.parameters[0].schema.type == 'integer'
        test2Op.parameters[0].schema.format == 'int32'
        test2Op.parameters[0].schema.enum
        test2Op.parameters[0].schema.enum[0] == 1
        test2Op.parameters[0].schema.enum[1] == 2
    }
}
