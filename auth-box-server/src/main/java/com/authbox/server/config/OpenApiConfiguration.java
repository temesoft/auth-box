package com.authbox.server.config;

import com.authbox.base.config.AppProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
// Below annotation is used to fix http vs https protocol issue when proxy does not forward X-Forwarded-Proto
// https://stackoverflow.com/questions/70843940/springdoc-openapi-ui-how-do-i-set-the-request-to-https
@OpenAPIDefinition(servers = {@Server(url = "/", description = "Default Server URL")})
public class OpenApiConfiguration {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String BASIC_AUTH = "basicAuth";

    private final AppProperties appProperties;

    @Bean
    OpenAPI customOpenAPI() {

        final String apiTitle = appProperties.getName().toUpperCase() + " API";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH).addList(BASIC_AUTH))
                .components(
                        new Components()
                                .addSecuritySchemes(BEARER_AUTH,
                                        new SecurityScheme()
                                                .name(BEARER_AUTH)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                )
                                .addSecuritySchemes(BASIC_AUTH,
                                        new SecurityScheme()
                                                .name(BEARER_AUTH)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("basic")
                                )
                )
                .info(new Info().title(apiTitle).version(appProperties.getVersion()));
    }
}
