package com.authbox.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class
})
@EnableCaching
public class Application {

    public static void main(final String... args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        SpringApplication.run(Application.class, args);
    }
}
