package com.authbox.server.config;

import com.authbox.base.service.AccessLogService;
import com.authbox.server.service.ScopeService;
import com.authbox.server.service.TokenEndpointProcessor;
import com.authbox.server.service.processor.AuthorizationCodeGrantTypeTokenEndpointProcessor;
import com.authbox.server.service.processor.ClientCredentialsGrantTypeTokenEndpointProcessor;
import com.authbox.server.service.processor.PasswordGrantTypeTokenEndpointProcessor;
import com.authbox.server.service.processor.RefreshTokenGrantTypeTokenEndpointProcessor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.Security;

@Configuration
public class EndpointProcessorConfiguration {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    TokenEndpointProcessor authorizationCodeGrantTypeTokenEndpointProcessor() {
        return new AuthorizationCodeGrantTypeTokenEndpointProcessor();
    }

    @Bean
    TokenEndpointProcessor clientCredentialsGrantTypeTokenEndpointProcessor(final ScopeService scopeService,
                                                                            final AccessLogService accessLogService) {
        return new ClientCredentialsGrantTypeTokenEndpointProcessor(scopeService, accessLogService);
    }

    @Bean
    TokenEndpointProcessor passwordGrantTypeTokenEndpointProcessor(final ScopeService scopeService) {
        return new PasswordGrantTypeTokenEndpointProcessor(scopeService);
    }

    @Bean
    TokenEndpointProcessor refreshTokenGrantTypeTokenEndpointProcessor() {
        return new RefreshTokenGrantTypeTokenEndpointProcessor();
    }
}
