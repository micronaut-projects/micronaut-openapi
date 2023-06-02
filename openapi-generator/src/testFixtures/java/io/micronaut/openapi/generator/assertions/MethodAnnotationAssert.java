package io.micronaut.openapi.generator.assertions;

import com.github.javaparser.ast.expr.AnnotationExpr;
import org.assertj.core.util.CanIgnoreReturnValue;

import java.util.List;

@CanIgnoreReturnValue
public class MethodAnnotationAssert extends AbstractAnnotationAssert<MethodAnnotationAssert> {

    private final MethodAssert methodAssert;
    private final ConstructorAssert constructorAssert;

    protected MethodAnnotationAssert(final MethodAssert methodAssert, final List<AnnotationExpr> annotationExpr) {
        super(annotationExpr);
        this.methodAssert = methodAssert;
        this.constructorAssert = null;
    }

    protected MethodAnnotationAssert(final ConstructorAssert constructorAssert, final List<AnnotationExpr> annotationExpr) {
        super(annotationExpr);
        this.constructorAssert = constructorAssert;
        this.methodAssert = null;
    }

    public MethodAssert toMethod() {
        if (methodAssert == null) {
            throw new IllegalArgumentException("No method assert for constructor's annotations");
        }
        return methodAssert;
    }

    public ConstructorAssert toConstructor() {
        if (constructorAssert == null) {
            throw new IllegalArgumentException("No constructor assert for method's annotations");
        }
        return constructorAssert;
    }

}
