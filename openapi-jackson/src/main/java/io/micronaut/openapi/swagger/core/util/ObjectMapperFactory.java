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
package io.micronaut.openapi.swagger.core.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.micronaut.openapi.swagger.core.jackson.ExampleSerializer;
import io.micronaut.openapi.swagger.core.jackson.MediaTypeSerializer;
import io.micronaut.openapi.swagger.core.jackson.Schema31Serializer;
import io.micronaut.openapi.swagger.core.jackson.SchemaSerializer;
import io.micronaut.openapi.swagger.core.jackson.mixin.Components31Mixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.ComponentsMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.DateSchemaMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.Discriminator31Mixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.DiscriminatorMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.ExampleMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.ExtensionsMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.InfoMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.LicenseMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.MediaTypeMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.OpenAPI31Mixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.OpenAPIMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.OperationMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.Schema31Mixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.links.LinkParameter;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.EncodingProperty;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.oas.models.tags.Tag;
import org.snakeyaml.engine.v2.api.LoadSettings;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import java.util.LinkedHashMap;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public class ObjectMapperFactory {

    private static final JacksonModule MODULE_SERIALIZATION_30 = new SimpleModule() {
        @Override
        public void setupModule(SetupContext context) {
            super.setupModule(context);
            context.addSerializerModifier(new ValueSerializerModifier() {
                @Override
                public ValueSerializer<?> modifySerializer(
                    SerializationConfig config, BeanDescription.Supplier desc, ValueSerializer<?> serializer) {
                    if (Schema.class.isAssignableFrom(desc.getBeanClass())) {
                        return new SchemaSerializer((ValueSerializer<Object>) serializer);
                    } else if (MediaType.class.isAssignableFrom(desc.getBeanClass())) {
                        return new MediaTypeSerializer((ValueSerializer<Object>) serializer);
                    } else if (Example.class.isAssignableFrom(desc.getBeanClass())) {
                        return new ExampleSerializer((ValueSerializer<Object>) serializer);
                    }
                    return serializer;
                }
            });
        }
    };

    private static final JacksonModule MODULE_SERIALIZATION_31 = new SimpleModule() {
        @Override
        public void setupModule(SetupContext context) {
            super.setupModule(context);
            context.addSerializerModifier(new ValueSerializerModifier() {
                @Override
                public ValueSerializer<?> modifySerializer(
                    SerializationConfig config, BeanDescription.Supplier desc, ValueSerializer<?> serializer) {
                    if (Schema.class.isAssignableFrom(desc.getBeanClass())) {
                        return new Schema31Serializer((ValueSerializer<Object>) serializer);
                    } else if (MediaType.class.isAssignableFrom(desc.getBeanClass())) {
                        return new MediaTypeSerializer((ValueSerializer<Object>) serializer);
                    } else if (Example.class.isAssignableFrom(desc.getBeanClass())) {
                        return new ExampleSerializer((ValueSerializer<Object>) serializer);
                    }
                    return serializer;
                }
            });
        }
    };

    private ObjectMapperFactory() {
    }

    public static ObjectMapper createJson() {
        return create(JsonMapper.builder(), false);
    }

    public static ObjectMapper createJson31() {
        return create(JsonMapper.builder(), true);
    }

    public static ObjectMapper createYaml() {
        return create(YAMLMapper.builder(buildYamlFactory()), false);
    }

    public static ObjectMapper createYaml31() {
        return create(YAMLMapper.builder(buildYamlFactory()), true);
    }

    private static YAMLFactory buildYamlFactory() {
        return YAMLFactory.builder()
            .loadSettings(LoadSettings.builder()
                .setAllowDuplicateKeys(false)
                .build())
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
            .enable(YAMLWriteFeature.SPLIT_LINES)
            .enable(YAMLWriteFeature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
            .build();
    }

    private static ObjectMapper create(MapperBuilder<?, ?> builder, boolean openapi31) {

        var sourceMixins = new LinkedHashMap<Class<?>, Class<?>>();

        sourceMixins.put(ApiResponses.class, ExtensionsMixin.class);
        sourceMixins.put(Contact.class, ExtensionsMixin.class);
        sourceMixins.put(Encoding.class, ExtensionsMixin.class);
        sourceMixins.put(EncodingProperty.class, ExtensionsMixin.class);
        sourceMixins.put(Example.class, ExampleMixin.class);
        sourceMixins.put(ExternalDocumentation.class, ExtensionsMixin.class);
        sourceMixins.put(Link.class, ExtensionsMixin.class);
        sourceMixins.put(LinkParameter.class, ExtensionsMixin.class);
        sourceMixins.put(MediaType.class, MediaTypeMixin.class);
        sourceMixins.put(OAuthFlow.class, ExtensionsMixin.class);
        sourceMixins.put(OAuthFlows.class, ExtensionsMixin.class);
        sourceMixins.put(Operation.class, OperationMixin.class);
        sourceMixins.put(PathItem.class, ExtensionsMixin.class);
        sourceMixins.put(Paths.class, ExtensionsMixin.class);
        sourceMixins.put(Scopes.class, ExtensionsMixin.class);
        sourceMixins.put(Server.class, ExtensionsMixin.class);
        sourceMixins.put(ServerVariable.class, ExtensionsMixin.class);
        sourceMixins.put(ServerVariables.class, ExtensionsMixin.class);
        sourceMixins.put(Tag.class, ExtensionsMixin.class);
        sourceMixins.put(XML.class, ExtensionsMixin.class);
        sourceMixins.put(ApiResponse.class, ExtensionsMixin.class);
        sourceMixins.put(Parameter.class, ExtensionsMixin.class);
        sourceMixins.put(RequestBody.class, ExtensionsMixin.class);
        sourceMixins.put(Header.class, ExtensionsMixin.class);
        sourceMixins.put(SecurityScheme.class, ExtensionsMixin.class);
        sourceMixins.put(Callback.class, ExtensionsMixin.class);

        if (!openapi31) {
            sourceMixins.put(Schema.class, SchemaMixin.class);
            sourceMixins.put(DateSchema.class, DateSchemaMixin.class);
            sourceMixins.put(Components.class, ComponentsMixin.class);
            sourceMixins.put(Info.class, InfoMixin.class);
            sourceMixins.put(License.class, LicenseMixin.class);
            sourceMixins.put(OpenAPI.class, OpenAPIMixin.class);
            sourceMixins.put(Discriminator.class, DiscriminatorMixin.class);
        } else {
            sourceMixins.put(Info.class, ExtensionsMixin.class);
            sourceMixins.put(Schema.class, Schema31Mixin.class);
            sourceMixins.put(Components.class, Components31Mixin.class);
            sourceMixins.put(OpenAPI.class, OpenAPI31Mixin.class);
            sourceMixins.put(DateSchema.class, DateSchemaMixin.class);
            sourceMixins.put(Discriminator.class, Discriminator31Mixin.class);
        }

        if (openapi31) {
            builder.addModules(MODULE_SERIALIZATION_31, new DeserializationModule31());
        } else {
            builder.addModules(MODULE_SERIALIZATION_30, new DeserializationModule());
        }

        return builder
            .addMixIns(sourceMixins)
            .enable(MapperFeature.USE_GETTERS_AS_SETTERS)
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .accessorNaming(new DefaultAccessorNamingStrategy.Provider()
                .withFirstCharAcceptance(true, true))
            .changeDefaultPropertyInclusion(incl -> incl
                .withValueInclusion(Include.NON_NULL)
                .withContentInclusion(Include.NON_NULL)
            )
            .build();
    }

    public static JsonMapper createConvertObjectMapper() {
        return createConvertObjectMapper(JsonMapper.builder(), false);
    }

    public static JsonMapper createConvertObjectMapper31() {
        return createConvertObjectMapper(JsonMapper.builder(), true);
    }

    private static JsonMapper createConvertObjectMapper(JsonMapper.Builder builder, boolean openapi31) {

        if (openapi31) {
            builder.addModules(MODULE_SERIALIZATION_31, new DeserializationModule31());
        } else {
            builder.addModules(MODULE_SERIALIZATION_30, new DeserializationModule());
        }

        return builder
            .enable(MapperFeature.USE_GETTERS_AS_SETTERS, MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, MapperFeature.DEFAULT_VIEW_INCLUSION)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .accessorNaming(new DefaultAccessorNamingStrategy.Provider()
                .withFirstCharAcceptance(true, true))
            .changeDefaultPropertyInclusion(incl -> incl
                .withValueInclusion(Include.NON_NULL)
                .withContentInclusion(Include.NON_NULL)
            )
            .build();
    }
}
