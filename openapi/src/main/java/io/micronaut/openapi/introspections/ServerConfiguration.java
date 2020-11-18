package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;

@Introspected(classes = {
		Server.class,
		ServerVariable.class,
		ServerVariables.class,
})
public class ServerConfiguration {
}
