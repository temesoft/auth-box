package com.authbox.web.config.security;

import com.authbox.base.dao.UserDao;
import com.authbox.web.config.MethodSecurityConfiguration;
import com.authbox.web.config.Oauth2ServerProperties;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static com.authbox.web.config.Constants.API_PREFIX;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
@Order(HIGHEST_PRECEDENCE)
@Import({Oauth2ServerProperties.class, MethodSecurityConfiguration.class})
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    public static final Map<String, String> ACCESS_PATH_PREFIX_AND_SCOPE_PREFIX = ImmutableMap.of(
            "/organization", "organization",
            "/oauth2-user", "oauth2-user",
            "/oauth2-token", "oauth2-token",
            "/oauth2-scope", "oauth2-scope",
            "/oauth2-client", "oauth2-client"
    );

    @Autowired
    private UserDao userDao;

    @Autowired
    private Oauth2ServerProperties oauth2ServerProperties;

    public static final String[] SECURE = {
            "/api/**",
            "/secure/**"
    };
    public static final String[] ALLOWED = {
            // standard
            "/",
            "/index.html",
            "/register.html",
            "/registration",
            "/css/**",
            "/img/**",
            "/js/**",
            "/error",
            "/logout",
            "/actuator", "/actuator/**",

            // swagger
            "/swagger-ui/*",
            "/swagger-ui/index.html",
            "/swagger-resources/**",
            "/v2/**",

            // h2 console
            "/h2-console",
            "/h2-console/**"};

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.headers().frameOptions().disable();
        http
                .authorizeRequests()
                .antMatchers(ALLOWED)
                .permitAll();
        http
                .logout()
                .logoutSuccessUrl("/")
                .logoutUrl("/logout")
                .and()
                .authorizeRequests(authorizeRequestsCustomizerSecure(false))
                .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/secure/index.html")
                .permitAll();

        http
                .authorizeRequests(authorizeRequestsCustomizerSecure(true))
                .oauth2ResourceServer()
                .opaqueToken()
                .introspectionUri(oauth2ServerProperties.getIntrospectionUri())
                .introspectionClientCredentials(oauth2ServerProperties.getClientId(), oauth2ServerProperties.getClientSecret());
    }

    @Override
    public void configure(final WebSecurity web) {
        web.ignoring().antMatchers("/webjars/**");
    }

    private Customizer<ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry> authorizeRequestsCustomizerSecure(final boolean isLastSetup) {
        return registry -> {
            ACCESS_PATH_PREFIX_AND_SCOPE_PREFIX.forEach((pathPatternPrefix, scopePrefix) -> {
                registry.antMatchers(HttpMethod.GET, API_PREFIX + pathPatternPrefix, API_PREFIX + pathPatternPrefix + "/**")
                        .access("hasAuthority('SCOPE_" + scopePrefix + "/read') || hasRole('USER') || hasRole('ADMIN')");
                registry.antMatchers(HttpMethod.GET, API_PREFIX + pathPatternPrefix + "/**/2fa-qr-code")
                        .access("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')");
                registry.antMatchers(HttpMethod.POST, API_PREFIX + pathPatternPrefix, API_PREFIX + pathPatternPrefix + "/**")
                        .access("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')");
                registry.antMatchers(HttpMethod.PUT, API_PREFIX + pathPatternPrefix, API_PREFIX + pathPatternPrefix + "/**")
                        .access("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')");
                registry.antMatchers(HttpMethod.DELETE, API_PREFIX + pathPatternPrefix, API_PREFIX + pathPatternPrefix + "/**")
                        .access("hasAuthority('SCOPE_" + scopePrefix + "/write') || hasRole('ADMIN')");
            });

            registry.antMatchers(SECURE).authenticated();
            if (isLastSetup) {
                registry.anyRequest().authenticated();
            }
        };
    }

    @Bean
    @Override
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
}