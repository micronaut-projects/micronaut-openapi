package io.micronaut.openapi.visitor;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.inject.ast.TypedElement;

/**
 * Some util methods.
 *
 * @since 4.8.3
 */
public final class TypeElementUtils {

    private TypeElementUtils() {
    }

    /**
     * Checks Nullable annotions / optinal type to understand that the element can be null.
     *
     * @param element typed element
     *
     * @return true if element is nullable, false - otherwise.
     */
    public static boolean isNullable(TypedElement element) {
        return element.isNullable()
            || element.getType().isOptional()
            || element.hasStereotype(Nullable.class)
            || element.hasStereotype("javax.annotation.Nullable")
            || element.hasStereotype("jakarta.annotation.Nullable")
            || element.hasStereotype("org.jetbrains.annotations.Nullable")
            || element.hasStereotype("androidx.annotation.Nullable")
            || element.hasStereotype("edu.umd.cs.findbugs.annotations.Nullable")
            || element.hasStereotype("org.eclipse.jdt.annotation.Nullable")
            || element.hasStereotype("io.reactivex.annotations.Nullable")
            || element.hasStereotype("io.reactivex.rxjava3.annotations.Nullable")
            || element.hasStereotype("reactor.util.annotation.Nullable")
            || element.hasStereotype("org.jspecify.annotations.Nullable");
    }
}
