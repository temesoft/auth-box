package com.authbox.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class
})
public class Application {
    public static void main(final String... args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        SpringApplication.run(Application.class, args);
    }
}
