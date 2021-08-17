package com.authbox.server.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.headers().frameOptions().disable();
        http
                .authorizeRequests()
                .antMatchers(
                        // standard
                        "/",
                        "/index.html",
                        "/css/**",
                        "/img/**",
                        "/js/**",
                        "/login",
                        "/error",
                        "/actuator", "/actuator/**",

                        // swagger
                        "/swagger-ui/*",
                        "/swagger-ui/index.html",
                        "/swagger-resources/**",
                        "/v2/**",

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
                .anyRequest()
                .authenticated();
    }
}