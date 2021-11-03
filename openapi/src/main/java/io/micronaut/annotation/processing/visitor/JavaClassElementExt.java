/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.annotation.processing.visitor;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.visitor.VisitorContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Internal class that copies private methods from {@link JavaClassElement} and it is used to get the right getters
 * and setter when using Lombok.
 */
@Internal
public class JavaClassElementExt {

    private final TypeElement typeElement;
    private final JavaClassElement classElement;
    private final JavaVisitorContext visitorContext;

    /**
     * Constructor.
     *
     * @param javaClassElement   The {@link JavaClassElement}
     * @param javaVisitorContext The {@link JavaVisitorContext}
     */
    public JavaClassElementExt(ClassElement javaClassElement,
                               VisitorContext javaVisitorContext) {
        this.classElement = (JavaClassElement) javaClassElement;
        this.visitorContext = (JavaVisitorContext) javaVisitorContext;
        this.typeElement = (TypeElement) javaClassElement.getNativeType();
    }

    /**
     * Returns fluent accessor methods.
     *
     * @return fluent accessor methods
     */
    public List<PropertyElement> fluentBeanProperties() {
        Map<String, BeanPropertyData> props = new LinkedHashMap<>();
        Map<String, VariableElement> fields = new LinkedHashMap<>();
        Elements elements = visitorContext.getElements();
        ElementFilter.fieldsIn(elements.getAllMembers(typeElement)).forEach(v -> fields.put(v.getSimpleName().toString(), v));
        Set<String> fieldNames = fields.keySet();
        ElementFilter.methodsIn(elements.getAllMembers(typeElement)).stream()
                // skip java.lang.Object methods
                .filter(method -> !isObjectClassMethod(method, elements))
                .filter(method -> isCandidateFluentBeanMethod(method, fieldNames))
                .forEach(executableElement -> fluentBeanProperty(props, executableElement));
        return processPropertyElements(props, fields);
    }

    private static boolean isObjectClassMethod(ExecutableElement method, Elements elements) {
        final TypeElement te = (TypeElement) method.getEnclosingElement();
        return te.equals(elements.getTypeElement("java.lang.Object"));
    }

    private boolean isCandidateFluentBeanMethod(ExecutableElement method, Set<String> fieldNames) {
        if (!checkModifiers(method)) {
            return false;
        }
        String methodName = method.getSimpleName().toString();
        return fieldNames.contains(methodName) && (method.getParameters().isEmpty() || method.getParameters().size() == 1);
    }

    private boolean checkModifiers(ExecutableElement method) {
        final Set<Modifier> modifiers = method.getModifiers();
        return method.getModifiers().contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.PRIVATE)
                && !method.getSimpleName().toString().contains("$");
    }

    private void fluentBeanProperty(Map<String, BeanPropertyData> props, ExecutableElement executableElement) {
        String methodName = executableElement.getSimpleName().toString();
        final TypeElement declaringTypeElement = (TypeElement) executableElement.getEnclosingElement();

        if (executableElement.getParameters().isEmpty()) {
            getterBeanProperty(props, executableElement, methodName, declaringTypeElement);
        } else if (executableElement.getParameters().size() == 1) {
            setterBeanProperty(props, executableElement, methodName, declaringTypeElement);
        }
    }

    private void getterBeanProperty(Map<String, BeanPropertyData> props, ExecutableElement executableElement, String methodName, final TypeElement declaringTypeElement) {
        String propertyName = NameUtils.getPropertyNameForGetter(methodName);
        TypeMirror returnType = executableElement.getReturnType();
        ClassElement getterReturnType;
        if (returnType instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) returnType;
            final String tvn = tv.toString();
            final ClassElement ce = classElement.getTypeArguments().get(tvn);
            if (ce != null) {
                getterReturnType = classElement;
            } else {
                getterReturnType = classElement.mirrorToClassElement(returnType, visitorContext, classElement.getGenericTypeInfo(), true);
            }
        } else {
            getterReturnType = classElement.mirrorToClassElement(returnType, visitorContext, classElement.getGenericTypeInfo(), true);
        }

        BeanPropertyData beanPropertyData = props.computeIfAbsent(propertyName, BeanPropertyData::new);
        configureDeclaringType(declaringTypeElement, beanPropertyData);
        beanPropertyData.type = getterReturnType;
        beanPropertyData.getter = executableElement;
        if (beanPropertyData.setter != null) {
            TypeMirror typeMirror = beanPropertyData.setter.getParameters().get(0).asType();
            ClassElement setterParameterType = classElement.mirrorToClassElement(typeMirror, visitorContext, classElement.getGenericTypeInfo(), true);
            if (!setterParameterType.getName().equals(getterReturnType.getName())) {
                beanPropertyData.setter = null; // not a compatible setter
            }
        }
    }

    private void setterBeanProperty(Map<String, BeanPropertyData> props, ExecutableElement executableElement, String methodName, final TypeElement declaringTypeElement) {
        String propertyName = NameUtils.getPropertyNameForSetter(methodName);
        TypeMirror typeMirror = executableElement.getParameters().get(0).asType();
        ClassElement setterParameterType = classElement.mirrorToClassElement(typeMirror, visitorContext, classElement.getGenericTypeInfo(), true);

        BeanPropertyData beanPropertyData = props.computeIfAbsent(propertyName, BeanPropertyData::new);
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

    private void configureDeclaringType(TypeElement declaringTypeElement, BeanPropertyData beanPropertyData) {
        if (beanPropertyData.declaringType == null && !classElement.equals(declaringTypeElement)) {
            beanPropertyData.declaringType = classElement.mirrorToClassElement(
                    declaringTypeElement.asType(),
                    visitorContext,
                    classElement.getGenericTypeInfo(),
                    true);
        } else if (beanPropertyData.declaringType == null) {
            beanPropertyData.declaringType = classElement.mirrorToClassElement(
                    declaringTypeElement.asType(),
                    visitorContext,
                    classElement.getGenericTypeInfo(),
                    false);
        }
    }

    private List<PropertyElement> processPropertyElements(Map<String, BeanPropertyData> props, Map<String, VariableElement> fields) {
        if (props.isEmpty()) {
            return Collections.emptyList();
        }
        List<PropertyElement> propertyElements = new ArrayList<>();
        for (Map.Entry<String, BeanPropertyData> entry : props.entrySet()) {
            String propertyName = entry.getKey();
            BeanPropertyData value = entry.getValue();
            final VariableElement fieldElement = fields.get(propertyName);
            if (value.getter != null) {
                final AnnotationMetadata annotationMetadata;
                if (fieldElement != null) {
                    annotationMetadata = visitorContext.getAnnotationUtils().getAnnotationMetadata(fieldElement, value.getter);
                } else {
                    annotationMetadata = visitorContext.getAnnotationUtils().newAnnotationBuilder().buildForMethod(value.getter);
                }

                propertyElements.add(toPropertyElement(propertyName, value, annotationMetadata));
            }
        }
        return Collections.unmodifiableList(propertyElements);
    }

    private JavaPropertyElement toPropertyElement(String propertyName, BeanPropertyData value, final AnnotationMetadata annotationMetadata) {
        return new JavaPropertyElement(
                value.declaringType == null ? classElement : value.declaringType,
                (Element) value.getter,
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
                            classElement,
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
                        classElement,
                        value.getter,
                        annotationMetadata,
                        visitorContext
                ));
            }
        };
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

        BeanPropertyData(String propertyName) {
            this.propertyName = propertyName;
        }
    }
}
