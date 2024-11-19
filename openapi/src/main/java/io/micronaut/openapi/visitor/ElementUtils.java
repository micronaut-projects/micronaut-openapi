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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Creator;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.RequestAttribute;
import io.micronaut.http.multipart.FileUpload;
import io.micronaut.inject.annotation.AnnotationMetadataHierarchy;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static io.micronaut.openapi.visitor.ConfigUtils.isJsonViewEnabled;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DEPRECATED;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_HIDDEN;

/**
 * Some util methods.
 *
 * @since 4.8.3
 */
@Internal
public final class ElementUtils {

    public static final AnnotationValue<?>[] EMPTY_ANNOTATION_VALUES_ARRAY = new AnnotationValue[0];

    public static final List<String> CONTAINER_TYPES = List.of(
        AtomicReference.class.getName(),
        "com.google.common.base.Optional",
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

    public static boolean isSingleResponseType(ClassElement returnType) {
        return (returnType.isAssignable("io.reactivex.Single")
            || returnType.isAssignable("io.reactivex.rxjava3.core.Single")
            || returnType.isAssignable("org.reactivestreams.Publisher"))
            && returnType.getFirstTypeArgument().isPresent()
            && isResponseType(returnType.getFirstTypeArgument().orElse(null));
    }

    public static boolean isResponseType(ClassElement returnType) {
        return returnType != null
            && (returnType.isAssignable(HttpResponse.class)
            || returnType.isAssignable("org.springframework.http.HttpEntity"));
    }

    /**
     * Checks Nullable annotations / optional type to understand that the element can be null.
     *
     * @param element typed element
     * @return true if element is nullable, false - otherwise.
     */
    public static boolean isNullable(TypedElement element) {

        var type = element.getType();

        return element.isNullable()
            || type.isOptional()
            || type.isAssignable(Optional.class)
            || type.isAssignable("com.google.common.base.Optional")
            || type.isAssignable(AtomicReference.class)
            || type.isAssignable(OptionalInt.class)
            || type.isAssignable(OptionalLong.class)
            || type.isAssignable(OptionalDouble.class)
            ;
    }

    /**
     * Checking if the type is file upload type.
     *
     * @param type type element
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
     * @return true if this type assignable with known file types
     */
    public static boolean isReturnTypeFile(ClassElement type) {
        return findAnyAssignable(type, FILE_TYPES);
    }

    /**
     * Checking if the type is container.
     *
     * @param type type element
     * @return true if this type assignable with known container types
     */
    public static boolean isContainerType(ClassElement type) {
        return findAnyAssignable(type, CONTAINER_TYPES);
    }

    /**
     * Checking if the type is void.
     *
     * @param type type element
     * @return true if this type assignable with known void types
     */
    public static boolean isVoid(ClassElement type) {
        return findAnyAssignable(type, VOID_TYPES);
    }

    /**
     * Checking if the type is void.
     *
     * @param type type element
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

        var schemaAnn = parameter.getAnnotation(Schema.class);
        boolean isHidden = schemaAnn != null && schemaAnn.booleanValue(PROP_HIDDEN).orElse(false);

        return isHidden
            || parameter.isAnnotationPresent(Hidden.class)
            || parameter.isAnnotationPresent(JsonIgnore.class)
            || parameter.isAnnotationPresent(Header.class) && parameter.getType().isAssignable(Map.class)
            || parameter.isAnnotationPresent(RequestAttribute.class)
            || parameter.booleanValue(Parameter.class, PROP_HIDDEN).orElse(false)
            || parameter.hasAnnotation("io.micronaut.session.annotation.SessionValue")
            || parameter.hasAnnotation("org.springframework.web.bind.annotation.RequestAttribute")
            || parameter.hasAnnotation("org.springframework.web.bind.annotation.SessionAttribute")
            || parameter.hasAnnotation("org.springframework.web.bind.annotation.SessionAttributes")
            || parameter.hasAnnotation("jakarta.ws.rs.core.Context")
            || isIgnoredParameterType(parameter.getType());
    }

    public static boolean isJavaBasicType(String typeName) {
        return ClassUtils.isJavaBasicType(typeName)
            || LocalTime.class.getName().equals(typeName)
            || OffsetTime.class.getName().equals(typeName)
            || OffsetDateTime.class.getName().equals(typeName)
            || Period.class.getName().equals(typeName)
            || YearMonth.class.getName().equals(typeName)
            || Year.class.getName().equals(typeName)
            || MonthDay.class.getName().equals(typeName)
            || ZoneId.class.getName().equals(typeName)
            || ZoneOffset.class.getName().equals(typeName)
            ;
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
            || parameterType.isAssignable("jakarta.servlet.ServletConfig")
            || parameterType.isAssignable("jakarta.servlet.ServletContext")
            || parameterType.isAssignable("jakarta.servlet.ServletRequest")
            || parameterType.isAssignable("jakarta.servlet.ServletResponse")

            // jax-rs
            || parameterType.isAssignable("jakarta.ws.rs.core.Application")
            || parameterType.isAssignable("jakarta.ws.rs.core.HttpHeaders")
            || parameterType.isAssignable("jakarta.ws.rs.core.Cookie")
            || parameterType.isAssignable("jakarta.ws.rs.core.Request")
            || parameterType.isAssignable("jakarta.ws.rs.core.SecurityContext")
            || parameterType.isAssignable("jakarta.ws.rs.core.UriInfo")
            || parameterType.isAssignable("jakarta.ws.rs.core.Configuration")
            || parameterType.isAssignable("jakarta.ws.rs.container.ResourceContext")
            || parameterType.isAssignable("jakarta.ws.rs.ext.Providers")

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

    public static AnnotationMetadata getAnnotationMetadata(Element el) {
        if (el == null) {
            return AnnotationMetadata.EMPTY_METADATA;
        }
        if (el instanceof MemberElement memberEl) {
            var propMetadata = memberEl.getAnnotationMetadata();
            AnnotationMetadata constructorMetadata = null;
            var constructor = getCreatorConstructor(memberEl.getOwningType());
            if (constructor != null) {
                for (var constructorParam : constructor.getParameters()) {
                    if (constructorParam.getName().equals(memberEl.getName())) {
                        constructorMetadata = constructorParam.getAnnotationMetadata();
                        break;
                    }
                }
            }
            if (constructorMetadata == null || constructorMetadata.isEmpty()) {
                return propMetadata;
            }
            return new AnnotationMetadataHierarchy(true, propMetadata, constructorMetadata);
        }
        return el.getAnnotationMetadata();
    }

    public static Optional<AnnotationValue<Annotation>> findAnnotation(Element el, String annName) {
        if (el == null) {
            return Optional.empty();
        }
        if (el instanceof MemberElement memberEl) {
            var result = memberEl.findAnnotation(annName);
            if (result.isPresent()) {
                return result;
            }
            var constructor = getCreatorConstructor(memberEl.getOwningType());
            if (constructor != null) {
                for (var constructorParam : constructor.getParameters()) {
                    if (constructorParam.getName().equals(memberEl.getName())) {
                        return constructorParam.findAnnotation(annName);
                    }
                }
            }
            return Optional.empty();
        }
        return el.findAnnotation(annName);
    }

    public static <T> boolean isAnnotationPresent(Element el, Class<T> annClass) {
        return isAnnotationPresent(el, annClass.getName());
    }

    public static boolean isAnnotationPresent(Element el, String annName) {
        if (el == null) {
            return false;
        }
        if (el instanceof MemberElement memberEl) {
            var result = memberEl.isAnnotationPresent(annName);
            if (result) {
                return true;
            }
            var constructor = getCreatorConstructor(memberEl.getOwningType());
            if (constructor != null) {
                for (var constructorParam : constructor.getParameters()) {
                    if (constructorParam.getName().equals(memberEl.getName())) {
                        return constructorParam.isAnnotationPresent(annName);
                    }
                }
            }
            return false;
        }
        return el.isAnnotationPresent(annName);
    }

    public static <T extends Annotation> Optional<String> stringValue(Element el, Class<T> annClass, String member) {
        if (el == null) {
            return Optional.empty();
        }
        if (el instanceof MemberElement memberEl) {
            var result = memberEl.stringValue(annClass, member);
            if (result.isPresent()) {
                return result;
            }
            var constructor = getCreatorConstructor(memberEl.getOwningType());
            if (constructor != null) {
                for (var constructorParam : constructor.getParameters()) {
                    if (constructorParam.getName().equals(memberEl.getName())) {
                        return constructorParam.stringValue(annClass, member);
                    }
                }
            }
            return result;
        }
        return el.stringValue(annClass, member);
    }

    public static <T extends Annotation> AnnotationValue<T> getAnnotation(Element el, Class<T> annClass) {
        return getAnnotation(el, annClass.getName());
    }

    public static AnnotationValue getAnnotation(Element el, String annName) {
        if (el == null) {
            return null;
        }
        if (el instanceof MemberElement memberEl) {
            var result = memberEl.getAnnotation(annName);
            if (result != null) {
                return result;
            }
            var constructor = getCreatorConstructor(memberEl.getOwningType());
            if (constructor != null) {
                for (var constructorParam : constructor.getParameters()) {
                    if (constructorParam.getName().equals(memberEl.getName())) {
                        return constructorParam.getAnnotation(annName);
                    }
                }
            }
            return result;
        }
        return el.getAnnotation(annName);
    }

    private static MethodElement getCreatorConstructor(ClassElement classEl) {

        var cachedConstructor = Utils.getCreatorConstructorsCache().get(classEl.getName());
        if (cachedConstructor != null) {
            return cachedConstructor;
        }

        var creatorConstructor = classEl.getPrimaryConstructor().orElse(null);
        var constructors = classEl.getAccessibleConstructors();
        if (constructors.size() > 1) {
            for (var constructor : constructors) {
                if (constructor.isDeclaredAnnotationPresent(JsonCreator.class)
                    || constructor.isDeclaredAnnotationPresent(Creator.class)) {
                    creatorConstructor = constructor;
                }
            }
        }
        Utils.getCreatorConstructorsCache().put(classEl.getName(), creatorConstructor);
        return creatorConstructor;
    }

    public static ClassElement getJsonViewClass(Element element, VisitorContext context) {
        if (!isJsonViewEnabled(context)) {
            return null;
        }
        var jsonViewAnn = element.findAnnotation(JsonView.class).orElse(null);
        if (jsonViewAnn != null) {
            String jsonViewClassName = jsonViewAnn.stringValue().orElse(null);
            if (jsonViewClassName != null) {
                return ContextUtils.getClassElement(jsonViewClassName, context);
            }
        }
        return null;
    }

    public static boolean isTypeWithGenericNullable(ClassElement type) {
        return type.isAssignable(Optional.class)
            || type.isAssignable("com.google.common.base.Optional")
            || type.isAssignable(AtomicReference.class)
            ;
    }

    public static boolean isDeprecated(Element el) {
        if (el == null) {
            return false;
        }
        // schema deprecated flag in swagger annotations is prioritized
        var schemaAnn = el.getAnnotation(Schema.class);
        var operationAnn = el.getAnnotation(Operation.class);
        var parameterAnn = el.getAnnotation(Parameter.class);
        var headerAnn = el.getAnnotation(Header.class);
        var deprecatedBySchema = schemaAnn != null ? schemaAnn.booleanValue(PROP_DEPRECATED).orElse(null) : null;
        var deprecatedByOperation = operationAnn != null ? operationAnn.booleanValue(PROP_DEPRECATED).orElse(null) : null;
        var deprecatedByParameter = parameterAnn != null ? parameterAnn.booleanValue(PROP_DEPRECATED).orElse(null) : null;
        var deprecatedByHeader = headerAnn != null ? headerAnn.booleanValue(PROP_DEPRECATED).orElse(null) : null;
        if ((deprecatedBySchema != null && !deprecatedBySchema)
            || (deprecatedByOperation != null && !deprecatedByOperation)
            || (deprecatedByParameter != null && !deprecatedByParameter)
            || (deprecatedByHeader != null && !deprecatedByHeader)
        ) {
            return false;
        }
        return deprecatedBySchema != null
            || deprecatedByOperation != null
            || deprecatedByParameter != null
            || deprecatedByHeader != null
            || el.isAnnotationPresent(Deprecated.class)
            || el.isAnnotationPresent("kotlin.Deprecated");
    }

    public static boolean isEnum(ClassElement classElement) {

        var isEnum = classElement.isEnum();

        var jsonFormatAnn = classElement.getAnnotation(JsonFormat.class);
        if (jsonFormatAnn == null) {
            return isEnum;
        }

        var jsonShape = jsonFormatAnn.get("shape", JsonFormat.Shape.class).orElse(JsonFormat.Shape.ANY);
        return jsonShape != JsonFormat.Shape.OBJECT && isEnum;
    }
}
