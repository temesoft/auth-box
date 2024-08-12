package com.authbox.server.config;


import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(conf -> conf.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                // standard
                                "/",
                                "/index.html",
                                "/css/**",
                                "/img/**",
                                "/js/**",
                                "/login",
                                "/error",
                                "/actuator",
                                "/actuator/**",

                                // swagger
                                "/swagger-ui/*",
                                "/swagger-ui/index.html",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",

                                // h2 console
                                "/h2-console",
                                "/h2-console/**",

                                // api
                                "/api/**",

                                // oauth
                                "/oauth/authorize",
                                "/oauth/authorize/scopes",
                                "/oauth/authorize/2fa",
                                "/oauth/authorize/finish",
                                "/oauth/token",
                                "/oauth/introspection",
                                "/oauth/user"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}