/*
 * Copyright 2017-2020 original authors
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

import static javax.lang.model.type.TypeKind.NONE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import io.micronaut.annotation.processing.AnnotationUtils;
import io.micronaut.annotation.processing.PublicMethodVisitor;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
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

    private static boolean sameType(String type, DeclaredType dt) {
        Element elt = dt.asElement();
        return (elt instanceof TypeElement) && type.equals(((TypeElement) elt).getQualifiedName().toString());
    }

    /**
     * Returns the return type of the method. Takes care of kotlin Continuation.
     *
     * @param method method.
     * @param visitorContext The visitor context.
     * @return A ClassElement.
     */
    public static ClassElement getReturnType(MethodElement method, VisitorContext visitorContext) {
        ParameterElement[] parameters = method.getParameters();
        boolean isSuspend = parameters.length > 0 && parameters[parameters.length - 1].getGenericType().isAssignable("kotlin.coroutines.Continuation");
        if (isSuspend) {
            return continuationReturnType(parameters[parameters.length - 1], visitorContext, Collections.emptyMap());
        }
        return method.getReturnType();
    }

    /**
     * Returns the return type of the method. Takes care of kotlin Continuation.
     *
     * @param method method.
     * @param visitorContext The visitor context.
     * @return A ClassElement.
     */
    public static ClassElement getGenericReturnType(MethodElement method, VisitorContext visitorContext) {
        ParameterElement[] parameters = method.getParameters();
        boolean isSuspend = parameters.length > 0 && parameters[parameters.length - 1].getGenericType().isAssignable("kotlin.coroutines.Continuation");
        if (isSuspend) {
            return continuationReturnType(parameters[parameters.length - 1], visitorContext, ((JavaClassElement) method.getOwningType()).getGenericTypeInfo());
        }
        return method.getGenericReturnType();
    }

    private static ClassElement continuationReturnType(ParameterElement parameter, VisitorContext visitorContext, Map<String, Map<String, TypeMirror>> info) {
        JavaVisitorContext jcontext = (JavaVisitorContext) visitorContext;
        VariableElement varElement = (VariableElement) parameter.getNativeType();
        DeclaredType dType = (DeclaredType) varElement.asType();
        WildcardType wType = (WildcardType) dType.getTypeArguments().iterator().next();
        TypeMirror tm = wType.getSuperBound();
        // check for Void
        if ((tm instanceof DeclaredType) && sameType("kotlin.Unit", (DeclaredType) tm)) {
            return new JavaVoidElement();
        } else {
            return ((JavaParameterElement) parameter).parameterizedClassElement(tm, jcontext, info);
        }
    }

    private static String findDocumentation(Set<TypeMirror> superTypes, ExecutableElement method, Elements elements, Types types) {
        String doc = elements.getDocComment(method);
        if (doc == null) {
            for (TypeMirror tm: superTypes) {
                TypeElement te = (TypeElement) types.asElement(tm);
                for (ExecutableElement m: ElementFilter.methodsIn(te.getEnclosedElements())) {
                    if (method.equals(m) || elements.hides(method, m) || elements.overrides(method, m, (TypeElement) method.getEnclosingElement())) {
                        doc = elements.getDocComment(m);
                        break;
                    }
                }
                if (doc != null) {
                    break;
                }
            }
        }
        return doc;
    }

    private static Set<TypeMirror> superTypes(TypeElement typeElement, Types types) {
        List<? extends TypeMirror> superTypes = types.directSupertypes(typeElement.asType());
        Set<TypeMirror> typeMirrors = new LinkedHashSet<>();
        for (TypeMirror tm: superTypes) {
            TypeElement te = (TypeElement) types.asElement(tm);
            // skip java.lang.Object
            if (!(te.getKind() == ElementKind.CLASS && te.getSuperclass().getKind() == NONE)) {
                typeMirrors.add(tm);
            }
        }
        for (TypeMirror tm: superTypes) {
            TypeElement te = (TypeElement) types.asElement(tm);
            // skip java.lang.Object
            if (!(te.getKind() == ElementKind.CLASS && te.getSuperclass().getKind() == NONE)) {
                typeMirrors.addAll(superTypes((TypeElement) types.asElement(tm), types));
            }
        }
        return typeMirrors;
    }

    private static boolean isObjectClassMethod(ExecutableElement method, Elements elements) {
        final TypeElement te = (TypeElement) method.getEnclosingElement();
        return te.equals(elements.getTypeElement("java.lang.Object"));
    }

    private static boolean isCandidateMethod(ExecutableElement method) {
        Set<Modifier> modifiers = method.getModifiers();
        return !modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.PRIVATE)
                && !method.getSimpleName().toString().contains("$");
    }

    /**
     * Returns the methods of this class.
     *
     * @return A list of methods.
     */
    public List<MethodElement> getCandidateMethods() {
        Elements elements = visitorContext.getElements();
        Types types = visitorContext.getTypes();
        AnnotationUtils annotations = visitorContext.getAnnotationUtils();
        Set<TypeMirror> superTypes = superTypes(classElement, types);
        return ElementFilter.methodsIn(elements.getAllMembers(classElement)).stream()
             // skip java.lang.Object methods
            .filter(method -> !isObjectClassMethod(method, elements))
            .filter(JavaClassElementExt::isCandidateMethod)
            .map(method -> new JavaMethodElementExt(this.javaClassElement, method,
                            annotations.getAnnotationMetadata(method), visitorContext, findDocumentation(superTypes, method, elements, types)))
            .collect(Collectors.toList());
    }

    /**
     * Returns fluent accessor methods.
     *
     * @return fluent accessor methods
     */
    public List<PropertyElement> getFluentBeanProperties() {
        List<BeanPropertyData> props = new ArrayList<>();
        Map<String, VariableElement> fields = new LinkedHashMap<>();
        Set<String> fieldNames = new HashSet<>();

        classElement.asType().accept(new FluentBeanPropertiesVisitor(visitorContext.getTypes(), fieldNames, props, fields), null);
        return processFields(props, fields);

    }

    private List<PropertyElement> processFields(List<BeanPropertyData> props, Map<String, VariableElement> fields) {
        if (props.isEmpty()) {
            return Collections.emptyList();
        }
        List<PropertyElement> propertyElements = new ArrayList<>();
        for (BeanPropertyData value : props) {
            String propertyName = value.propertyName;

            if (value.getter != null) {
                final AnnotationMetadata annotationMetadata;
                final VariableElement fieldElement = fields.get(propertyName);
                if (fieldElement == null) {
                    annotationMetadata = visitorContext.getAnnotationUtils().newAnnotationBuilder()
                            .buildForMethod(value.getter);
                } else {
                    annotationMetadata = visitorContext.getAnnotationUtils().getAnnotationMetadata(fieldElement,
                            value.getter);
                }

                JavaPropertyElement propertyElement = new JavaPropertyElement(
                        value.declaringType == null ? this.javaClassElement : value.declaringType, value.getter,
                        annotationMetadata, propertyName, value.type, value.setter == null, visitorContext) {
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
                                    JavaClassElementExt.this.javaClassElement, value.setter, visitorContext
                                            .getAnnotationUtils().newAnnotationBuilder().buildForMethod(value.setter),
                                    visitorContext));
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<MethodElement> getReadMethod() {
                        return Optional.of(new JavaMethodElement(JavaClassElementExt.this.javaClassElement,
                                value.getter, annotationMetadata, visitorContext));
                    }
                };
                propertyElements.add(propertyElement);
            }
        }
        return Collections.unmodifiableList(propertyElements);
    }

    /**
     * Find fluent bean properties.
     * @author croudet
     * @since 1.5.1
     */
    private final class FluentBeanPropertiesVisitor extends PublicMethodVisitor<Object, Object> {
        private final Set<String> fieldNames;
        private final List<BeanPropertyData> props;
        private final Map<String, VariableElement> fields;

        private FluentBeanPropertiesVisitor(Types types, Set<String> fieldNames, List<BeanPropertyData> props,
                Map<String, VariableElement> fields) {
            super(types);
            this.fieldNames = fieldNames;
            this.props = props;
            this.fields = fields;
        }

        @Override
        protected boolean isAcceptable(Element element) {
            if (element.getKind() == ElementKind.FIELD) {
                fieldNames.add(element.getSimpleName().toString());
                return true;
            }
            if (element.getKind() == ElementKind.METHOD && element instanceof ExecutableElement) {
                Set<Modifier> modifiers = element.getModifiers();
                if (modifiers.contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.STATIC)
                        && !modifiers.contains(Modifier.ABSTRACT)) {
                    ExecutableElement executableElement = (ExecutableElement) element;
                    String methodName = executableElement.getSimpleName().toString();
                    if (methodName.contains("$")) {
                        return false;
                    }

                    return executableElement.getParameters().isEmpty()
                            || executableElement.getParameters().size() == 1;
                }
            }
            return false;
        }

        private BeanPropertyData computeIfAbsent(String propertyName) {
            for (BeanPropertyData bpd : props) {
                if (propertyName.equals(bpd.propertyName)) {
                    return bpd;
                }
            }
            BeanPropertyData bpd = new BeanPropertyData(propertyName);
            props.add(bpd);
            return bpd;
        }

        @Override
        protected void accept(DeclaredType declaringType, Element element, Object o) {
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
                processGetter(methodName, executableElement, declaringTypeElement);
            } else if (executableElement.getParameters().size() == 1) {
                processSetter(methodName, executableElement, declaringTypeElement);
            }
        }

        private void processSetter(String methodName, ExecutableElement executableElement,
                final TypeElement declaringTypeElement) {
            String propertyName = methodName;
            TypeMirror typeMirror = executableElement.getParameters().get(0).asType();
            ClassElement setterParameterType = mirrorToClassElement(typeMirror, visitorContext,
                    JavaClassElementExt.this.genericTypeInfo);
            BeanPropertyData beanPropertyData = computeIfAbsent(propertyName);
            configureDeclaringType(declaringTypeElement, beanPropertyData);
            ClassElement propertyType = beanPropertyData.type;
            if (propertyType == null) {
                beanPropertyData.setter = executableElement;
            } else {
                if (propertyType.getName().equals(setterParameterType.getName())) {
                    beanPropertyData.setter = executableElement;
                }
            }
        }

        private void processGetter(String methodName, ExecutableElement executableElement,
                final TypeElement declaringTypeElement) {
            String propertyName = methodName;
            TypeMirror returnType = executableElement.getReturnType();
            ClassElement getterReturnType;
            if (returnType instanceof TypeVariable) {
                TypeVariable tv = (TypeVariable) returnType;
                final String tvn = tv.toString();
                final ClassElement clElement = getTypeArguments().get(tvn);
                if (clElement == null) {
                    getterReturnType = mirrorToClassElement(returnType, visitorContext,
                            JavaClassElementExt.this.genericTypeInfo);
                } else {
                    getterReturnType = clElement;
                }
            } else {
                getterReturnType = mirrorToClassElement(returnType, visitorContext,
                        JavaClassElementExt.this.genericTypeInfo);
            }

            BeanPropertyData beanPropertyData = computeIfAbsent(propertyName);
            configureDeclaringType(declaringTypeElement, beanPropertyData);
            beanPropertyData.type = getterReturnType;
            beanPropertyData.getter = executableElement;
            if (beanPropertyData.setter != null) {
                TypeMirror typeMirror = beanPropertyData.setter.getParameters().get(0).asType();
                ClassElement setterParameterType = mirrorToClassElement(typeMirror, visitorContext,
                        JavaClassElementExt.this.genericTypeInfo);
                if (!setterParameterType.getName().equals(getterReturnType.getName())) {
                    beanPropertyData.setter = null; // not a compatible
                                                    // setter
                }
            }
        }

        private void configureDeclaringType(TypeElement declaringTypeElement, BeanPropertyData beanPropertyData) {
            if (beanPropertyData.declaringType == null && !classElement.equals(declaringTypeElement)) {
                beanPropertyData.declaringType = mirrorToClassElement(declaringTypeElement.asType(), visitorContext,
                        genericTypeInfo);
            }
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

        BeanPropertyData(String propertyName) {
            this.propertyName = propertyName;
        }
    }
}
