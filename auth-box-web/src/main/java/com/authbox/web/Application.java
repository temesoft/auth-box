package com.authbox.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {
        HibernateJpaAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
public class Application {
    public static void main(final String... args) {
        SpringApplication.run(Application.class, args);
    }
}
