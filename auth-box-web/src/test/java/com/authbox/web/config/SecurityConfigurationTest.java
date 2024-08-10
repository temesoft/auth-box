package com.authbox.web.config;

import com.authbox.web.config.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityConfigurationTest {

    public static final String PASSWORD_TEXT = "admin";
    public static final String PASSWORD_HASH = "$2a$10$BUN/uL0ZVcvTclADYjtpMe1God6nzYnm3t7iISfP/i5yP79.sDQLO";

    @Test
    public void testStandardPasswordHashing() {
        final SecurityConfiguration configuration = new SecurityConfiguration();
        final PasswordEncoder passwordEncoder = configuration.passwordEncoder();
        assertThat(passwordEncoder).isNotNull();
        assertThat(passwordEncoder.encode(PASSWORD_TEXT)).isNotBlank().hasSize(60).startsWith("$"); // BCrypt format
        assertThat(passwordEncoder.matches(PASSWORD_TEXT, PASSWORD_HASH)).isTrue(); // encoded password and known hash should match
        assertThat(passwordEncoder.encode(PASSWORD_TEXT)).isNotEqualTo(PASSWORD_HASH); // each hash of same password should be different

        assertThat(passwordEncoder.matches("tester", "$2a$10$14VcPh6ctZ9zQ64CGjXtnO.rKce4obEkPEJ4n0Jz9J.76qTa3Wuw2")).isTrue();
        assertThat(passwordEncoder.matches("qweqwe", "$2a$10$th30RQRD177ka5.JWvXTqe2vX2faH9UEnrIz25kormL6DNvotu7BW")).isTrue();
    }
}