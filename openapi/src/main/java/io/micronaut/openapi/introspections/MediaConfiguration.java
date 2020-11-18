package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.EmailSchema;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.EncodingProperty;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.PasswordSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.media.XML;

@Introspected(classes = {
		ArraySchema.class,
		BinarySchema.class,
		BooleanSchema.class,
		ByteArraySchema.class,
		ComposedSchema.class,
		Content.class,
		DateSchema.class,
		DateTimeSchema.class,
		Discriminator.class,
		EmailSchema.class,
		Encoding.class,
		EncodingProperty.class,
		FileSchema.class,
		IntegerSchema.class,
		MapSchema.class,
		MediaType.class,
		NumberSchema.class,
		ObjectSchema.class,
		PasswordSchema.class,
		Schema.class,
		StringSchema.class,
		UUIDSchema.class,
		XML.class,
})
public class MediaConfiguration {
}
