package com.authbox.server;

import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

import static org.springframework.boot.Banner.Mode.OFF;
import static org.springframework.boot.WebApplicationType.NONE;

public class SpringContextTest {

    public static ConfigurableApplicationContext startSpringApplication(final Map<String, Object> props, final Class<?>... classes) {
        val app = new SpringApplication(
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
}
