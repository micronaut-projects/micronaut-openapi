package io.micronaut.openapi.generator.assertions;

import com.github.javaparser.ast.expr.AnnotationExpr;
import org.assertj.core.util.CanIgnoreReturnValue;

import java.util.List;

@CanIgnoreReturnValue
public class PropertyAnnotationAssert extends AbstractAnnotationAssert<PropertyAnnotationAssert> {

    private final PropertyAssert propertyAssert;

    protected PropertyAnnotationAssert(final PropertyAssert propertyAssert, final List<AnnotationExpr> annotationExpr) {
        super(annotationExpr);
        this.propertyAssert = propertyAssert;
    }

    public PropertyAssert toProperty() {
        return propertyAssert;
    }
}
