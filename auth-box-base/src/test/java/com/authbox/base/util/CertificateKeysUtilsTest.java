package com.authbox.base.util;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class CertificateKeysUtilsTest {

    @Test
    public void testGenerateRsaKeyPair() {
        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        assertThat(rsaKeyPair).isNotNull();
        assertThat(rsaKeyPair.privateKeyPem).isNotEmpty().contains("BEGIN PRIVATE KEY");
        assertThat(rsaKeyPair.publicKeyPem).isNotEmpty().contains("BEGIN PUBLIC KEY");
    }

    @Test
    public void testGeneratePrivateKey() {
        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        val privateKey = CertificateKeysUtils.generatePrivateKey(rsaKeyPair.privateKeyPem);
        assertThat(privateKey).isNotNull().isInstanceOf(PrivateKey.class);
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    }

    @Test
    public void testGeneratePublicKey() {
        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        val publicKey = CertificateKeysUtils.generatePublicKey(rsaKeyPair.publicKeyPem);
        assertThat(publicKey).isNotNull().isInstanceOf(PublicKey.class);
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(publicKey.getFormat()).isEqualTo("X.509");
    }

    @Test
    void testGenerateRsaKeyPairHandlesExecutionFailure() {
        try (MockedStatic<ExecUtils> mockedExec = mockStatic(ExecUtils.class)) {
            mockedExec.when(() -> ExecUtils.executeCommand(anyString()))
                    .thenThrow(new InterruptedException("Simulated failure"));
            try {
                CertificateKeysUtils.generateRsaKeyPair();
            } catch (Exception e) {
                assertThat(e).isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Unable to generate rsa key pair");
            }
        }
    }
}