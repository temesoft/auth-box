package com.authbox.server;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

import static org.springframework.boot.Banner.Mode.OFF;
import static org.springframework.boot.WebApplicationType.NONE;

public class SpringContextTest {

    public static ConfigurableApplicationContext startSpringApplication(final Class<?>... classes) {
        return startSpringApplication(ImmutableMap.of(), classes);
    }

    public static ConfigurableApplicationContext startSpringApplication(final Map<String, Object> props, final Class<?>... classes) {
        final SpringApplication app = new SpringApplication(
                ArrayUtils.addAll(
                        new Class<?>[]{
                                ConfigurationPropertiesAutoConfiguration.class,
                                PropertyPlaceholderAutoConfiguration.class,},
                        classes
                )
        );
        app.setWebApplicationType(NONE);
        app.setBannerMode(OFF);
        app.setDefaultProperties(props);
        return app.run();
    }

    public static ConfigurableApplicationContext startSpringWebApplication(final Map<String, Object> props, final Class<?>... classes) {
        final SpringApplication app = new SpringApplication(
                ArrayUtils.addAll(
                        new Class<?>[]{
                                ConfigurationPropertiesAutoConfiguration.class,
                                PropertyPlaceholderAutoConfiguration.class,
                                ServletWebServerFactoryAutoConfiguration.class,
                                JacksonAutoConfiguration.class
                        },
                        classes
                )
        );
        app.setWebApplicationType(WebApplicationType.SERVLET);
        app.setBannerMode(OFF);
        app.setDefaultProperties(
                ImmutableMap.<String, Object>builder()
                        .putAll(props)
                        .put("server.port", "0")
                        .build()
        );
        return app.run();
    }

    public static int getPort(final ApplicationContext context) {
        return Integer.parseInt(context.getEnvironment().getProperty("local.server.port", "-1"));
    }
}
