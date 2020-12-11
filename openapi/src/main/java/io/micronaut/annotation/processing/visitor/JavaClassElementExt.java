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

import io.micronaut.annotation.processing.AnnotationUtils;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.ast.*;
import io.micronaut.inject.visitor.VisitorContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.stream.Collectors;

import static javax.lang.model.type.TypeKind.NONE;

/**
 * A class element returning data from a {@link TypeElement}.
 *
 * Implementation note: [smell] this class needs to track changes to {@link JavaClassElement}.
 * For example: {@link JavaClassElement} introduced {@link JavaClassElement#arrayDimensions}
 * resulting in a processing bug in this library when this class didn't account for it in the constructor.
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
    private final Map<String, Map<String, TypeMirror>> genericTypeInfo;

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
        super((TypeElement) jce.getNativeType(), jce.getAnnotationMetadata(), visitorContext, jce.getGenericTypeInfo(),
            jce.getArrayDimensions());
        this.javaClassElement = jce;
        this.classElement = (TypeElement) jce.getNativeType();
        this.visitorContext = visitorContext;
        this.genericTypeInfo = jce.getGenericTypeInfo();
    }

    @Override
    public ClassElement withArrayDimensions(int arrayDimensions) {
        return new JavaClassElementExt(javaClassElement.withArrayDimensions(arrayDimensions), visitorContext);
    }

    private static boolean sameType(String type, DeclaredType dt) {
        final Element elt = dt.asElement();
        return elt instanceof TypeElement && type.equals(((TypeElement) elt).getQualifiedName().toString());
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(visitorContext.getElements().getDocComment(classElement));
    }

    @Override
    public Optional<ClassElement> getSuperType() {
        TypeElement te = (TypeElement) getNativeType();
        TypeMirror dt = te.getSuperclass();
        // if super type has type arguments, then build a parameterized ClassElement
        if (dt instanceof DeclaredType && !((DeclaredType) dt).getTypeArguments().isEmpty()) {
            ClassElement sup = parameterizedClassElement(dt, visitorContext, visitorContext.getGenericUtils().buildGenericTypeArgumentElementInfo(te));
            return Optional.of(new JavaClassElementExt(sup, visitorContext));
        }
        return super.getSuperType();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JavaClassElementExt) {
            return javaClassElement.equals(((JavaClassElementExt) o).javaClassElement);
        }
        return javaClassElement.equals(o);
    }

    @Override
    public int hashCode() {
        return javaClassElement.hashCode();
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
        if (tm instanceof DeclaredType && sameType("kotlin.Unit", (DeclaredType) tm)) {
            return PrimitiveElement.VOID;
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

    private boolean isCandidateBeanMethod(ExecutableElement method) {

        if (!checkModifiers(method)) {
            return false;
        }
        String methodName = method.getSimpleName().toString();
        if (methodName.contains("$")) {
            return false;
        }

        if (NameUtils.isGetterName(methodName) && method.getParameters().isEmpty()) {
            return true;
        } else {
            return NameUtils.isSetterName(methodName) && method.getParameters().size() == 1;
        }
    }

    private boolean checkModifiers(ExecutableElement method) {
        final Set<Modifier> modifiers = method.getModifiers();
        return method.getModifiers().contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.PRIVATE)
                && !method.getSimpleName().toString().contains("$");
    }

    private boolean isCandidateFluentBeanMethod(ExecutableElement method, Set<String> fieldNames) {
        if (!checkModifiers(method)) {
            return false;
        }
        String methodName = method.getSimpleName().toString();
        return fieldNames.contains(methodName)  && (method.getParameters().isEmpty() || method.getParameters().size() == 1);
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
     * Returns the bean properties.
     *
     * @return The bean properties.
     */
    public List<PropertyElement> beanProperties() {
        Map<String, BeanPropertyData> props = new LinkedHashMap<>();
        Map<String, VariableElement> fields = new LinkedHashMap<>();
        Elements elements = visitorContext.getElements();
        ElementFilter.fieldsIn(elements.getAllMembers(classElement)).forEach(v -> fields.put(v.getSimpleName().toString(), v));
        ElementFilter.methodsIn(elements.getAllMembers(classElement)).stream()
                // skip java.lang.Object methods
                .filter(method -> !isObjectClassMethod(method, elements))
                .filter(this::isCandidateBeanMethod)
                .forEach(executableElement -> beanProperty(props, executableElement));
        return processPropertyElements(props, fields);
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
        ElementFilter.fieldsIn(elements.getAllMembers(classElement)).forEach(v -> fields.put(v.getSimpleName().toString(), v));
        Set<String> fieldNames = fields.keySet();
        ElementFilter.methodsIn(elements.getAllMembers(classElement)).stream()
                // skip java.lang.Object methods
                .filter(method -> !isObjectClassMethod(method, elements))
                .filter(method -> isCandidateFluentBeanMethod(method, fieldNames))
                .forEach(executableElement -> fluentBeanProperty(props, executableElement));
        return processPropertyElements(props, fields);
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
                if (fieldElement == null) {
                    annotationMetadata = visitorContext
                            .getAnnotationUtils()
                            .newAnnotationBuilder().buildForMethod(value.getter);
                } else {
                    annotationMetadata = visitorContext.getAnnotationUtils().getAnnotationMetadata(fieldElement, value.getter);
                }
                propertyElements.add(toPropertyElement(propertyName, value, annotationMetadata));
            }
        }
        return Collections.unmodifiableList(propertyElements);
    }

    private JavaPropertyElement toPropertyElement(String propertyName, BeanPropertyData value, final AnnotationMetadata annotationMetadata) {
        return new JavaPropertyElement(
                value.declaringType == null ? this : value.declaringType,
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
                            JavaClassElementExt.this,
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
                        JavaClassElementExt.this,
                        value.getter,
                        annotationMetadata,
                        visitorContext
                ));
            }
        };
    }

    private void beanProperty(Map<String, BeanPropertyData> props, ExecutableElement executableElement) {
        String methodName = executableElement.getSimpleName().toString();
        final TypeElement declaringTypeElement = (TypeElement) executableElement.getEnclosingElement();

        if (NameUtils.isGetterName(methodName) && executableElement.getParameters().isEmpty()) {
            getterBeanProperty(props, executableElement, methodName, declaringTypeElement);
        } else if (NameUtils.isSetterName(methodName) && executableElement.getParameters().size() == 1) {
            setterBeanProperty(props, executableElement, methodName, declaringTypeElement);
        }
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
            final ClassElement ce = getTypeArguments().get(tvn);
            if (ce == null) {
                getterReturnType = mirrorToClassElement(returnType, visitorContext, this.genericTypeInfo);
            } else {
                getterReturnType = ce;
            }
        } else {
            getterReturnType = mirrorToClassElement(returnType, visitorContext, this.genericTypeInfo);
        }

        BeanPropertyData beanPropertyData = props.computeIfAbsent(propertyName, BeanPropertyData::new);
        configureDeclaringType(declaringTypeElement, beanPropertyData);
        beanPropertyData.type = getterReturnType;
        beanPropertyData.getter = executableElement;
        if (beanPropertyData.setter != null) {
            TypeMirror typeMirror = beanPropertyData.setter.getParameters().get(0).asType();
            ClassElement setterParameterType = mirrorToClassElement(typeMirror, visitorContext, this.genericTypeInfo);
            if (!setterParameterType.getName().equals(getterReturnType.getName())) {
                beanPropertyData.setter = null; // not a compatible setter
            }
        }
    }

    private void setterBeanProperty(Map<String, BeanPropertyData> props, ExecutableElement executableElement, String methodName, final TypeElement declaringTypeElement) {
        String propertyName = NameUtils.getPropertyNameForSetter(methodName);
        TypeMirror typeMirror = executableElement.getParameters().get(0).asType();
        ClassElement setterParameterType = mirrorToClassElement(typeMirror, visitorContext, this.genericTypeInfo);

        BeanPropertyData beanPropertyData = props.computeIfAbsent(propertyName, BeanPropertyData::new);
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

    private void configureDeclaringType(TypeElement declaringTypeElement, BeanPropertyData beanPropertyData) {
        if (beanPropertyData.declaringType == null && !classElement.equals(declaringTypeElement)) {
            beanPropertyData.declaringType = mirrorToClassElement(
                    declaringTypeElement.asType(),
                    visitorContext,
                    genericTypeInfo
            );
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
