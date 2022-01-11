package com.authbox.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class
})
@EnableCaching
public class Application {

    private static ConfigurableApplicationContext applicationContext;

    public static void main(final String... args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        applicationContext = SpringApplication.run(Application.class, args);
    }

    private static ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
