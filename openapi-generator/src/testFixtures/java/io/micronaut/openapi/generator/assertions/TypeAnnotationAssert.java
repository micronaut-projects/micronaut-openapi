package io.micronaut.openapi.generator.assertions;

import com.github.javaparser.ast.expr.AnnotationExpr;
import org.assertj.core.util.CanIgnoreReturnValue;

import java.util.List;

@CanIgnoreReturnValue
public class TypeAnnotationAssert extends AbstractAnnotationAssert<TypeAnnotationAssert> {

    private final JavaFileAssert fileAssert;

    protected TypeAnnotationAssert(final JavaFileAssert fileAssert, final List<AnnotationExpr> annotationExpr) {
        super(annotationExpr);
        this.fileAssert = fileAssert;
    }

    public JavaFileAssert toType() {
        return fileAssert;
    }
}
