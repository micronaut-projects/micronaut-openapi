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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.VisitorContext;

/**
 * Some util methods.
 *
 * @since 4.8.3
 */
@Internal
public final class ElementUtils {

    public static final List<String> CONTAINER_TYPES = Arrays.asList(
            Optional.class.getName(),
            Future.class.getName(),
            "org.reactivestreams.Publisher",
            "io.reactivex.Single",
            "io.reactivex.Observable",
            "io.reactivex.Maybe",
            "io.reactivex.rxjava3.core.Single",
            "io.reactivex.rxjava3.core.Observable",
            "io.reactivex.rxjava3.core.Maybe",
            "kotlinx.coroutines.flow.Flow"
    );

    public static final List<String> FILE_TYPES = Arrays.asList(
            // this class from micronaut-http-server
            "io.micronaut.http.server.types.files.FileCustomizableResponseType",
            File.class.getName(),
            InputStream.class.getName(),
            ByteBuffer.class.getName()
    );

    public static final List<String> VOID_TYPES = Arrays.asList(
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
        return classElement != null &&
                "io.micronaut.annotation.processing.visitor.JavaClassElement".equals(classElement.getClass().getName()) &&
                "io.micronaut.annotation.processing.visitor.JavaVisitorContext".equals(context.getClass().getName());
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

    /**
     * Checking if the type is file upload type.
     *
     * @param type type element
     *
     * @return true if this type one of known file upload types
     */
    public static boolean isFileUpload(ClassElement type) {
        String typeName = type.getName();
        return "io.micronaut.http.multipart.StreamingFileUpload".equals(typeName)
                || "io.micronaut.http.multipart.CompletedFileUpload".equals(typeName)
                || "io.micronaut.http.multipart.CompletedPart".equals(typeName)
                || "io.micronaut.http.multipart.PartData".equals(typeName);
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
}
