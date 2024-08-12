package com.authbox.web.config.security;

import com.authbox.base.dao.UserDao;
import com.authbox.web.config.MethodSecurityConfiguration;
import com.authbox.web.config.Oauth2ServerProperties;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static com.authbox.web.config.Constants.API_PREFIX;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

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

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.headers(config -> config.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        http.authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    auth.requestMatchers(ALLOWED).permitAll();

                    ACCESS_PATH_PREFIX_AND_SCOPE_PREFIX.forEach((pathPatternPrefix, scopePrefix) -> {
                        auth.requestMatchers(antMatcher(HttpMethod.GET, API_PREFIX + pathPatternPrefix),
                                        antMatcher(HttpMethod.GET, API_PREFIX + pathPatternPrefix + "/**"))
                                .access(webEx("hasAuthority('SCOPE_" + scopePrefix + "/read') || hasRole('USER') || hasRole('ADMIN')"));

                        auth.requestMatchers(antMatcher(HttpMethod.GET, API_PREFIX + pathPatternPrefix + "/**/2fa-qr-code"))
                                .access(webEx("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')"));

                        auth.requestMatchers(antMatcher(HttpMethod.POST, API_PREFIX + pathPatternPrefix),
                                        antMatcher(HttpMethod.POST, API_PREFIX + pathPatternPrefix + "/**"))
                                .access(webEx("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')"));

                        auth.requestMatchers(antMatcher(HttpMethod.PUT, API_PREFIX + pathPatternPrefix),
                                        antMatcher(HttpMethod.PUT, API_PREFIX + pathPatternPrefix + "/**"))
                                .access(webEx("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')"));

                        auth.requestMatchers(antMatcher(HttpMethod.DELETE, API_PREFIX + pathPatternPrefix),
                                        antMatcher(HttpMethod.DELETE, API_PREFIX + pathPatternPrefix + "/**"))
                                .access(webEx("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')"));

                    });

                    auth.requestMatchers(Arrays.stream(SECURE)
                            .map((Function<String, RequestMatcher>) AntPathRequestMatcher::antMatcher)
                            .toArray(RequestMatcher[]::new)).authenticated();
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
                .defaultSuccessUrl("/secure/index.html")
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