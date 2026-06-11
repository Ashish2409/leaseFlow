package com.abm.leaseFlow.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "LeaseFlow API",
        version     = "v1",
        description = "Multi-tenant residential lease lifecycle management platform",
        contact     = @Contact(name = "LeaseFlow Engineering", email = "eng@leaseflow.io")
    ),
    servers = {
        @Server(url = "/", description = "Current server")
    }
)
@SecurityScheme(
    name            = "BearerAuth",
    type            = SecuritySchemeType.HTTP,
    scheme          = "bearer",
    bearerFormat    = "JWT",
    description     = "JWT access token. Obtain from POST /api/v1/auth/login"
)
public class OpenApiConfig {
}
