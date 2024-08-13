package com.authbox.server.config;

import com.authbox.base.config.AppProperties;
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
public class OpenApiConfiguration {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String BASIC_AUTH = "basicAuth";

    private final AppProperties appProperties;

    @Bean
    OpenAPI customOpenAPI() {

        final String apiTitle = appProperties.getName() + " API";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH).addList(BASIC_AUTH))
                .components(
                        new Components()
                                .addSecuritySchemes(BEARER_AUTH,
                                        new SecurityScheme()
                                                .name(BEARER_AUTH)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
//                                                .bearerFormat("JWT")
                                )
                                .addSecuritySchemes(BASIC_AUTH,
                                        new SecurityScheme()
                                                .name(BEARER_AUTH)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("basic")
//                                                .bearerFormat("JWT")
                                )
                )
                .info(new Info().title(apiTitle).version(appProperties.getVersion()));
    }
}
