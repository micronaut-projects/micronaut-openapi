package io.micronaut.configuration.openapi.docs.schemas

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema
// end::imports[]

// tag::clazz[]
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
@Schema(name = "MyPet", description = "Pet description")
annotation class MyAnn
// end::clazz[]
