package com.authbox.server.config;

import com.fasterxml.classmate.TypeResolver;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

import static com.authbox.base.config.Constants.OAUTH_PREFIX;
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

    @GetMapping
    @Timed
    void redirect(final HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.sendRedirect("/swagger-ui/index.html");
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
                .paths(PathSelectors.ant(OAUTH_PREFIX + "/**"))
                .build()
                .pathMapping("/")
                .ignoredParameterTypes(ModelAndView.class, View.class)
                .directModelSubstitute(LocalDate.class, String.class);
//                .additionalModels(typeResolver.resolve(AvailityOutgoing278Dto.class))
//                .additionalModels(typeResolver.resolve(AvailityOutgoing275Dto.class))
//                .additionalModels(typeResolver.resolve(AvailityIncomingDto.class))
//                .tags(new Tag(SOF_TAG, "Creates, retrieves SoF session / event entries"))
//                .tags(new Tag(EDI_278_TAG, "Creates, retrieves, updates EDI 278 entries"))
//                .tags(new Tag(EDI_275_TAG, "Creates, retrieves EDI 275 entries"));
    }

}
