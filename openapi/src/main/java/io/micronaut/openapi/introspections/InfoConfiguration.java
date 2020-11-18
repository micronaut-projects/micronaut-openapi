package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Introspected(classes = {
		Contact.class,
		Info.class,
		License.class,
})
public class InfoConfiguration {
}
