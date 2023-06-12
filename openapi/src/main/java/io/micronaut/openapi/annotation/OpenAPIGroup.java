package io.micronaut.openapi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.micronaut.context.annotation.AliasFor;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @since 4.9.1
 */
@Retention(RUNTIME)
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD})
public @interface OpenAPIGroup {

    /**
     * @return The names of the OpenAPi groups.
     */
    @AliasFor(member = "value")
    String[] names() default {};

    /**
     * @return The names of the OpenAPi groups to exclude endpoints from.
     */
    String[] excluded() default {};
}
