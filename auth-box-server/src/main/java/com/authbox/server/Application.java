package com.authbox.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = {
        HibernateJpaAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@EnableCaching
public class Application {

    private static ConfigurableApplicationContext applicationContext;

    public static void main(final String... args) {
        applicationContext = SpringApplication.run(Application.class, args);
    }

    private static ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }
}