package com.authbox.base.util;

import com.authbox.base.model.RsaKeyPair;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;

public class CertificateKeysUtilsTest {

    @Test
    public void testGenerateRsaKeyPair() {
        final RsaKeyPair rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        assertThat(rsaKeyPair).isNotNull();
        assertThat(rsaKeyPair.privateKeyPem).isNotEmpty();
        assertThat(rsaKeyPair.publicKeyPem).isNotEmpty();
    }

    @Test
    public void testGeneratePrivateKey() {
        final RsaKeyPair rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        final PrivateKey privateKey = CertificateKeysUtils.generatePrivateKey(rsaKeyPair.privateKeyPem);
        assertThat(privateKey).isNotNull();
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    }

    @Test
    public void testGeneratePublicKey() {
        final RsaKeyPair rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        final PublicKey publicKey = CertificateKeysUtils.generatePublicKey(rsaKeyPair.publicKeyPem);
        assertThat(publicKey).isNotNull();
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(publicKey.getFormat()).isEqualTo("X.509");
    }
}