package com.authbox.server.config;

import com.fasterxml.classmate.TypeResolver;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    // TODO: Below is a workaround for springfox 3.x compatibility with 2.6.x
    // https://stackoverflow.com/questions/70178343/springfox-3-0-0-is-not-working-with-spring-boot-2-6-0
    // TODO: Please remove when no longer needed.
    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            private <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(final List<T> mappings) {
                final List<T> copy = mappings.stream()
                        .filter(mapping -> mapping.getPatternParser() == null)
                        .collect(Collectors.toList());
                mappings.clear();
                mappings.addAll(copy);
            }

            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(final Object bean) {
                try {
                    final Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }
}
