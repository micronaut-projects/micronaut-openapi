package io.micronaut.openapi.generator.assertions;

import com.github.javaparser.ast.expr.AnnotationExpr;
import org.assertj.core.util.CanIgnoreReturnValue;

import java.util.List;

//CHECKSTYLE:OFF
@CanIgnoreReturnValue
public class ParameterAnnotationAssert extends AbstractAnnotationAssert<ParameterAnnotationAssert> {

    private final ParameterAssert parameterAssert;

    protected ParameterAnnotationAssert(final ParameterAssert parameterAssert, final List<AnnotationExpr> annotationExpr) {
        super(annotationExpr);
        this.parameterAssert = parameterAssert;
    }

    public ParameterAssert toParameter() {
        return parameterAssert;
    }
}
