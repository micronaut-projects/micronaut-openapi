/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.openapi.visitor;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.multipart.FileUpload;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Some util methods.
 *
 * @since 4.8.3
 */
@Internal
public final class ElementUtils {

    public static final List<String> CONTAINER_TYPES = List.of(
        Optional.class.getName(),
        Future.class.getName(),
        Callable.class.getName(),
        CompletionStage.class.getName(),
        "org.reactivestreams.Publisher",
        "io.reactivex.Single",
        "io.reactivex.Observable",
        "io.reactivex.Maybe",
        "io.reactivex.rxjava3.core.Single",
        "io.reactivex.rxjava3.core.Observable",
        "io.reactivex.rxjava3.core.Maybe",
        "kotlinx.coroutines.flow.Flow",
        "org.springframework.web.context.request.async.DeferredResult"
    );

    public static final List<String> FILE_TYPES = List.of(
        // this class from micronaut-http-server
        "io.micronaut.http.server.types.files.FileCustomizableResponseType",
        File.class.getName(),
        InputStream.class.getName(),
        ByteBuffer.class.getName()
    );

    public static final List<String> VOID_TYPES = List.of(
        void.class.getName(),
        Void.class.getName(),
        "kotlin.Unit"
    );

    private ElementUtils() {
    }

    /**
     * Returns true if classElement is a JavaClassElement.
     *
     * @param classElement A ClassElement.
     * @param context The context.
     *
     * @return true if classElement is a JavaClassElement.
     */
    public static boolean isJavaElement(ClassElement classElement, VisitorContext context) {
        return classElement != null
            && "io.micronaut.annotation.processing.visitor.JavaClassElement".equals(classElement.getClass().getName())
            && context != null
            && "io.micronaut.annotation.processing.visitor.JavaVisitorContext".equals(context.getClass().getName());
    }

    /**
     * Checks Nullable annotations / optional type to understand that the element can be null.
     *
     * @param element typed element
     *
     * @return true if element is nullable, false - otherwise.
     */
    public static boolean isNullable(TypedElement element) {
        return element.isNullable()
            || element.getType().isOptional();
    }

    public static boolean isAnnotationPresent(Element element, String className) {
        return element.findAnnotation(className).isPresent();
    }

    /**
     * Checking if the type is file upload type.
     *
     * @param type type element
     *
     * @return true if this type one of known file upload types
     */
    public static boolean isFileUpload(ClassElement type) {
        if (ElementUtils.isContainerType(type)) {
            var typeArg = type.getFirstTypeArgument().orElse(null);
            if (typeArg != null) {
                type = typeArg;
            }
        }
        String typeName = type.getName();
        return type.isAssignable(FileUpload.class)
            || "io.micronaut.http.multipart.StreamingFileUpload".equals(typeName)
            || "io.micronaut.http.multipart.CompletedFileUpload".equals(typeName)
            || "io.micronaut.http.multipart.CompletedPart".equals(typeName)
            || "io.micronaut.http.multipart.PartData".equals(typeName)
            || "org.springframework.web.multipart.MultipartFile".equals(typeName);
    }

    /**
     * Checking if the element not nullable.
     *
     * @param element element
     *
     * @return true if element is not nullable
     */
    public static boolean isNotNullable(Element element) {
        return element.isAnnotationPresent("javax.validation.constraints.NotNull$List")
            || element.isAnnotationPresent("jakarta.validation.constraints.NotNull$List")
            || element.isAnnotationPresent("javax.validation.constraints.NotBlank$List")
            || element.isAnnotationPresent("jakarta.validation.constraints.NotBlank$List")
            || element.isAnnotationPresent("javax.validation.constraints.NotEmpty$List")
            || element.isAnnotationPresent("jakarta.validation.constraints.NotEmpty$List")
            || element.isNonNull()
            || element.booleanValue(JsonProperty.class, "required").orElse(false);
    }

    /**
     * Checking if the type is file.
     *
     * @param type type element
     *
     * @return true if this type assignable with known file types
     */
    public static boolean isReturnTypeFile(ClassElement type) {
        return findAnyAssignable(type, FILE_TYPES);
    }

    /**
     * Checking if the type is container.
     *
     * @param type type element
     *
     * @return true if this type assignable with known container types
     */
    public static boolean isContainerType(ClassElement type) {
        return findAnyAssignable(type, CONTAINER_TYPES);
    }

    /**
     * Checking if the type is void.
     *
     * @param type type element
     *
     * @return true if this type assignable with known void types
     */
    public static boolean isVoid(ClassElement type) {
        return findAnyAssignable(type, VOID_TYPES);
    }

    /**
     * Checking if the type is void.
     *
     * @param type type element
     *
     * @return true if this type assignable with known container and type argument is void
     */
    public static boolean isReactiveAndVoid(ClassElement type) {
        return type.isAssignable("io.reactivex.Completable")
            || type.isAssignable("io.reactivex.rxjava3.core.Completable")
            || (isContainerType(type) && type.getFirstTypeArgument().isPresent() && isVoid(type.getFirstTypeArgument().get()));
    }

    private static boolean findAnyAssignable(ClassElement type, List<String> typeNames) {
        for (String typeName : typeNames) {
            if (type.isAssignable(typeName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIgnoredParameter(TypedElement parameter) {

        AnnotationValue<Schema> schemaAnn = parameter.getAnnotation(Schema.class);
        boolean isHidden = schemaAnn != null && schemaAnn.booleanValue("hidden").orElse(false);

        return isHidden
            || parameter.isAnnotationPresent(Hidden.class)
            || parameter.isAnnotationPresent(JsonIgnore.class)
            || parameter.booleanValue(Parameter.class, "hidden").orElse(false)
            || isAnnotationPresent(parameter, "io.micronaut.session.annotation.SessionValue")
            || isAnnotationPresent(parameter, "org.springframework.web.bind.annotation.SessionAttribute")
            || isAnnotationPresent(parameter, "org.springframework.web.bind.annotation.SessionAttributes")
            || isIgnoredParameterType(parameter.getType());
    }

    public static boolean isIgnoredParameterType(ClassElement parameterType) {
        return parameterType == null
            || parameterType.isAssignable(Principal.class)
            || parameterType.isAssignable("io.micronaut.session.Session")
            || parameterType.isAssignable("io.micronaut.security.authentication.Authentication")
            || parameterType.isAssignable("io.micronaut.http.HttpHeaders")
            || parameterType.isAssignable("kotlin.coroutines.Continuation")
            || parameterType.isAssignable(HttpRequest.class)
            || parameterType.isAssignable("io.micronaut.http.BasicAuth")
            // servlet API
            || parameterType.isAssignable("jakarta.servlet.http.HttpServletRequest")
            || parameterType.isAssignable("jakarta.servlet.http.HttpServletResponse")
            || parameterType.isAssignable("jakarta.servlet.http.HttpSession")
            || parameterType.isAssignable("jakarta.servlet.http.PushBuilder")
            // spring
            || parameterType.isAssignable("java.io.Reader")
            || parameterType.isAssignable("java.io.OutputStream")
            || parameterType.isAssignable("java.io.Writer")
            || parameterType.isAssignable("org.springframework.web.util.UriComponentsBuilder")
            || parameterType.isAssignable("org.springframework.web.bind.support.SessionStatus")
            || parameterType.isAssignable("org.springframework.web.context.request.RequestAttributes")
            || parameterType.isAssignable("org.springframework.http.HttpEntity")
            || parameterType.isAssignable("org.springframework.http.HttpMethod")
            || parameterType.isAssignable("org.springframework.validation.BindingResult")
            || parameterType.isAssignable("org.springframework.validation.Errors")
            ;
    }
}
