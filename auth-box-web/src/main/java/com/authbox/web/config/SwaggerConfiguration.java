package com.authbox.web.config;

import com.fasterxml.classmate.TypeResolver;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.authbox.web.config.Constants.API_PREFIX;
import static org.springframework.boot.actuate.trace.http.Include.AUTHORIZATION_HEADER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

@Configuration
@ConditionalOnProperty(value = "useSwagger", matchIfMissing = true)
@EnableSwagger2
@Controller
public class SwaggerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerConfiguration.class);

    @Autowired
    private TypeResolver typeResolver;

    public SwaggerConfiguration() {
        LOGGER.info("Swagger is enabled");
    }

    @Bean
    public Docket docketDefinition(@Value("${info.app.name}") final String appName,
                                   @Value("${info.app.description}") final String appDescription,
                                   @Value("${info.app.version}") final String appVersion) {

        return new Docket(SWAGGER_2)
                .apiInfo(new ApiInfo(
                        appName,
                        appDescription,
                        appVersion,
                        null,
                        null,
                        null,
                        null,
                        new ArrayList<>()
                ))
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.ant(API_PREFIX + "/**"))
                .build()
                .pathMapping("/")
                .ignoredParameterTypes(Authentication.class)
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Lists.newArrayList(apiKey()))
                .directModelSubstitute(LocalDate.class, String.class);
    }


    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(PathSelectors.ant(API_PREFIX + "/**"))
                .build();
    }

    private ApiKey apiKey() {
        return new ApiKey("Bearer", AUTHORIZATION, "header");
    }

    private List<SecurityReference> defaultAuth() {
        final AuthorizationScope authorizationScope = new AuthorizationScope("organization/read", "Organization read functionality");
        final AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Lists.newArrayList(new SecurityReference("Bearer", authorizationScopes));
    }
}
