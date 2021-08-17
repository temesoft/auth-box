package com.authbox.server.config;

import com.authbox.server.service.TokenEndpointProcessor;
import com.authbox.server.service.processor.AuthorizationCodeGrantTypeTokenEndpointProcessor;
import com.authbox.server.service.processor.ClientCredentialsGrantTypeTokenEndpointProcessor;
import com.authbox.server.service.processor.PasswordGrantTypeTokenEndpointProcessor;
import com.authbox.server.service.processor.RefreshTokenGrantTypeTokenEndpointProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class EndpointProcessorConfiguration {

    public EndpointProcessorConfiguration() {
        java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
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
    TokenEndpointProcessor clientCredentialsGrantTypeTokenEndpointProcessor() {
        return new ClientCredentialsGrantTypeTokenEndpointProcessor();
    }

    @Bean
    TokenEndpointProcessor passwordGrantTypeTokenEndpointProcessor() {
        return new PasswordGrantTypeTokenEndpointProcessor();
    }

    @Bean
    TokenEndpointProcessor refreshTokenGrantTypeTokenEndpointProcessor() {
        return new RefreshTokenGrantTypeTokenEndpointProcessor();
    }

}
