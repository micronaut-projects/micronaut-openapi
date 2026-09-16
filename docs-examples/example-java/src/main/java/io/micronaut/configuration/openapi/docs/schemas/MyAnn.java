package io.micronaut.configuration.openapi.docs.schemas;

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
// end::imports[]

// tag::clazz[]
@Documented
@Retention(RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Schema(name = "MyPet", description = "Pet description")
@interface MyAnn {
}
// end::clazz[]
