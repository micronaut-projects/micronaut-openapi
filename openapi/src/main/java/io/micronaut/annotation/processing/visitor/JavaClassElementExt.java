/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.annotation.processing.visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;

import io.micronaut.annotation.processing.PublicMethodVisitor;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.visitor.VisitorContext;

/**
 * A class element returning data from a {@link TypeElement}.
 *
 * @author James Kleeh
 * @author graemerocher
 * @author croudet
 * @since 1.0
 */
@Internal
public class JavaClassElementExt extends JavaClassElement {
    private final JavaClassElement javaClassElement;
    private final TypeElement classElement;
    private final JavaVisitorContext visitorContext;
    private Map<String, Map<String, TypeMirror>> genericTypeInfo;

    /**
     * @param ce       The {@link ClassElement}
     * @param visitorContext     The visitor context
     */
    public JavaClassElementExt(ClassElement ce, VisitorContext visitorContext) {
        this((JavaClassElement) ce, (JavaVisitorContext) visitorContext);
    }

    /**
     * @param jce       The {@link TypeElement}
     * @param visitorContext     The visitor context
     */
    private JavaClassElementExt(JavaClassElement jce, JavaVisitorContext visitorContext) {
        super((TypeElement) jce.getNativeType(), jce.getAnnotationMetadata(), visitorContext);
        this.javaClassElement = jce;
        this.classElement = (TypeElement) jce.getNativeType();
        this.visitorContext = visitorContext;
        this.genericTypeInfo = jce.getGenericTypeInfo();
    }

    /**
     * Returns fluent accessor methods.
     * @return fluent accessor methods
     */
    public List<PropertyElement> getFluentBeanProperties() {
        List<BeanPropertyData> props = new ArrayList<>();
        Map<String, VariableElement> fields = new LinkedHashMap<>();
        Set<String> fieldNames = new HashSet<>();

        classElement.asType().accept(new PublicMethodVisitor<Object, Object>(visitorContext.getTypes()) {

            @Override
            protected boolean isAcceptable(javax.lang.model.element.Element element) {
                if (element.getKind() == ElementKind.FIELD) {
                    fieldNames.add(element.getSimpleName().toString());
                    return true;
                }
                if (element.getKind() == ElementKind.METHOD && element instanceof ExecutableElement) {
                    Set<Modifier> modifiers = element.getModifiers();
                    if (modifiers.contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.ABSTRACT)) {
                        ExecutableElement executableElement = (ExecutableElement) element;
                        String methodName = executableElement.getSimpleName().toString();
                        if (methodName.contains("$")) {
                            return false;
                        }

                        return executableElement.getParameters().isEmpty() || executableElement.getParameters().size() == 1;
                    }
                }
                return false;
            }

            private BeanPropertyData computeIfAbsent(String propertyName) {
                for (BeanPropertyData bpd: props) {
                    if (propertyName.equals(bpd.propertyName)) {
                        return bpd;
                    }
                }
                BeanPropertyData bpd = new BeanPropertyData(propertyName);
                props.add(bpd);
                return bpd;
            }

            @Override
            protected void accept(DeclaredType declaringType, javax.lang.model.element.Element element, Object o) {
                if (element instanceof VariableElement) {
                    fields.put(element.getSimpleName().toString(), (VariableElement) element);
                    return;
                }

                String methodName = element.getSimpleName().toString();
                if (!fieldNames.contains(methodName)) {
                    return;
                }
                ExecutableElement executableElement = (ExecutableElement) element;
                final TypeElement declaringTypeElement = (TypeElement) executableElement.getEnclosingElement();

                if (executableElement.getParameters().isEmpty()) {
                    String propertyName = methodName;
                    TypeMirror returnType = executableElement.getReturnType();
                    ClassElement getterReturnType;
                    if (returnType instanceof TypeVariable) {
                        TypeVariable tv = (TypeVariable) returnType;
                        final String tvn = tv.toString();
                        final ClassElement clElement = getTypeArguments().get(tvn);
                        if (clElement != null) {
                            getterReturnType = clElement;
                        } else {
                            getterReturnType = mirrorToClassElement(returnType, visitorContext, JavaClassElementExt.this.genericTypeInfo);
                        }
                    } else {
                        getterReturnType = mirrorToClassElement(returnType, visitorContext, JavaClassElementExt.this.genericTypeInfo);
                    }

                    BeanPropertyData beanPropertyData = computeIfAbsent(propertyName);
                    configureDeclaringType(declaringTypeElement, beanPropertyData);
                    beanPropertyData.type = getterReturnType;
                    beanPropertyData.getter = executableElement;
                    if (beanPropertyData.setter != null) {
                        TypeMirror typeMirror = beanPropertyData.setter.getParameters().get(0).asType();
                        ClassElement setterParameterType = mirrorToClassElement(typeMirror, visitorContext, JavaClassElementExt.this.genericTypeInfo);
                        if (!setterParameterType.getName().equals(getterReturnType.getName())) {
                            beanPropertyData.setter = null; // not a compatible setter
                        }
                    }
                } else if (executableElement.getParameters().size() == 1) {
                    String propertyName = methodName;
                    TypeMirror typeMirror = executableElement.getParameters().get(0).asType();
                    ClassElement setterParameterType = mirrorToClassElement(typeMirror, visitorContext, JavaClassElementExt.this.genericTypeInfo);
                    BeanPropertyData beanPropertyData = computeIfAbsent(propertyName);
                    configureDeclaringType(declaringTypeElement, beanPropertyData);
                    ClassElement propertyType = beanPropertyData.type;
                    if (propertyType != null) {
                        if (propertyType.getName().equals(setterParameterType.getName())) {
                            beanPropertyData.setter = executableElement;
                        }
                    } else {
                        beanPropertyData.setter = executableElement;
                    }
                }
            }

            private void configureDeclaringType(TypeElement declaringTypeElement, BeanPropertyData beanPropertyData) {
                if (beanPropertyData.declaringType == null && !classElement.equals(declaringTypeElement)) {
                    beanPropertyData.declaringType = mirrorToClassElement(
                            declaringTypeElement.asType(),
                            visitorContext,
                            genericTypeInfo
                    );
                }
            }
        }, null);
        if (!props.isEmpty()) {
            List<PropertyElement> propertyElements = new ArrayList<>();
            for (BeanPropertyData value : props) {
                String propertyName = value.propertyName;

                if (value.getter != null) {
                    final AnnotationMetadata annotationMetadata;
                    final VariableElement fieldElement = fields.get(propertyName);
                    if (fieldElement != null) {
                        annotationMetadata = visitorContext.getAnnotationUtils().getAnnotationMetadata(fieldElement, value.getter);
                    } else {
                        annotationMetadata = visitorContext
                                .getAnnotationUtils()
                                .newAnnotationBuilder().buildForMethod(value.getter);
                    }

                    JavaPropertyElement propertyElement = new JavaPropertyElement(
                            value.declaringType == null ? this.javaClassElement : value.declaringType,
                            value.getter,
                            annotationMetadata,
                            propertyName,
                            value.type,
                            value.setter == null,
                            visitorContext) {
                        @Override
                        public Optional<String> getDocumentation() {
                            Elements elements = visitorContext.getElements();
                            String docComment = elements.getDocComment(value.getter);
                            return Optional.ofNullable(docComment);
                        }

                        @Override
                        public Optional<MethodElement> getWriteMethod() {
                            if (value.setter != null) {
                                return Optional.of(new JavaMethodElement(
                                        JavaClassElementExt.this.javaClassElement,
                                        value.setter,
                                        visitorContext.getAnnotationUtils().newAnnotationBuilder().buildForMethod(value.setter),
                                        visitorContext
                                ));
                            }
                            return Optional.empty();
                        }

                        @Override
                        public Optional<MethodElement> getReadMethod() {
                            return Optional.of(new JavaMethodElement(
                                    JavaClassElementExt.this.javaClassElement,
                                    value.getter,
                                    annotationMetadata,
                                    visitorContext
                            ));
                        }
                    };
                    propertyElements.add(propertyElement);
                }
            }
            return Collections.unmodifiableList(propertyElements);
        } else {
            return Collections.emptyList();
        }

    }

    /**
     * Internal holder class for getters and setters.
     */
    private static class BeanPropertyData {
        ClassElement type;
        ClassElement declaringType;
        ExecutableElement getter;
        ExecutableElement setter;
        final String propertyName;

        public BeanPropertyData(String propertyName) {
            this.propertyName = propertyName;
        }
    }
}
