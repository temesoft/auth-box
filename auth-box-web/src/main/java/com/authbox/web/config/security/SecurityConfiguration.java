package com.authbox.web.config.security;

import com.authbox.base.dao.UserDao;
import com.authbox.web.config.MethodSecurityConfiguration;
import com.authbox.web.config.Oauth2ServerProperties;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.DispatcherType;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.util.Map;

import static com.authbox.web.config.Constants.API_PREFIX;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
@Order(HIGHEST_PRECEDENCE)
@Import({Oauth2ServerProperties.class, MethodSecurityConfiguration.class})
public class SecurityConfiguration {

    private static final Map<String, String> ACCESS_PATH_PREFIX_AND_SCOPE_PREFIX = Map.of(
            "/organization", "organization",
            "/oauth2-user", "oauth2-user",
            "/oauth2-token", "oauth2-token",
            "/oauth2-scope", "oauth2-scope",
            "/oauth2-client", "oauth2-client"
    );
    private static final String[] SECURE = {
            "/api/**",
            "/secure/**"
    };
    private static final String[] ALLOWED = {
            "/",
            "/index.html",
            "/register.html",
            "/registration",
            "/sign-in.html",
            "/css/**",
            "/img/**",
            "/js/**",
            "/webjars/**",
            "/error",
            "/logout",
            "/actuator",
            "/actuator/**",

            // swagger
            "/swagger-ui/*",
            "/swagger-ui/index.html",
            "/swagger-resources/**",
            "/v3/api-docs/**",

            // h2 console
            "/h2-console",
            "/h2-console/**"};

    @Autowired
    private UserDao userDao;
    @Autowired
    private Oauth2ServerProperties oauth2ServerProperties;

    @ConditionalOnMissingBean
    @Bean
    AuthSuccessUrl defaultAuthSuccessUrl() {
        return () -> "/secure/index.html";
    }

    @ConditionalOnMissingBean
    @Bean
    SavedRequestAwareAuthenticationSuccessHandler successHandlerFinalStep(final AuthSuccessUrl authSuccessUrl) {
        val handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl(authSuccessUrl.url());
        handler.setRedirectStrategy((request, response, url) -> {
            if (url.contains("/.well-known/appspecific/")) {
                response.setHeader("Location", authSuccessUrl.url());
            } else {
                response.setHeader("Location", url);
            }
            response.setStatus(HttpStatus.FOUND.value());
            response.getWriter().flush();
        });
        return handler;
    }

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http,
                                           final AuthSuccessUrl authSuccessUrl,
                                           final SavedRequestAwareAuthenticationSuccessHandler successHandlerFinalStep) {
        http.csrf(AbstractHttpConfigurer::disable);
        http.headers(config -> config.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        http.authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    auth.requestMatchers(ALLOWED).permitAll();

                    ACCESS_PATH_PREFIX_AND_SCOPE_PREFIX.forEach((pathPatternPrefix, scopePrefix) -> {
                        auth.requestMatchers(HttpMethod.GET, API_PREFIX + pathPatternPrefix,
                                        API_PREFIX + pathPatternPrefix + "/**")
                                .access(webEx("hasAuthority('SCOPE_" + scopePrefix + "/read') || hasRole('USER') || hasRole('ADMIN')"));

                        auth.requestMatchers(HttpMethod.GET, API_PREFIX + pathPatternPrefix + "/*/2fa-qr-code")
                                .access(webEx("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')"));

                        auth.requestMatchers(HttpMethod.POST, API_PREFIX + pathPatternPrefix,
                                        API_PREFIX + pathPatternPrefix + "/**")
                                .access(webEx("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')"));

                        auth.requestMatchers(HttpMethod.PUT, API_PREFIX + pathPatternPrefix,
                                        API_PREFIX + pathPatternPrefix + "/**")
                                .access(webEx("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')"));

                        auth.requestMatchers(HttpMethod.DELETE, API_PREFIX + pathPatternPrefix,
                                        API_PREFIX + pathPatternPrefix + "/**")
                                .access(webEx("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')"));

                    });

                    auth.requestMatchers(SECURE).authenticated();
                    auth.anyRequest().authenticated();
                }
        );

        http.logout(config -> config
                .logoutSuccessUrl("/")
                .logoutUrl("/logout")
                .clearAuthentication(true)
                .invalidateHttpSession(true));

        http.formLogin(config -> config
                .loginPage("/login")
                .failureUrl("/sign-in.html?error")
                .defaultSuccessUrl(authSuccessUrl.url())
                .successHandler(successHandlerFinalStep)
                .permitAll());

        http.oauth2ResourceServer(config ->
                config.opaqueToken(
                        customizer -> customizer
                                .introspectionUri(oauth2ServerProperties.getIntrospectionUri())
                                .introspectionClientCredentials(oauth2ServerProperties.getClientId(), oauth2ServerProperties.getClientSecret())
                )
        );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            @Timed("loadUserByUsername")
            public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
                return userDao.getByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private WebExpressionAuthorizationManager webEx(final String value) {
        return new WebExpressionAuthorizationManager(value);
    }
}