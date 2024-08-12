package com.authbox.base.util;

import com.authbox.base.model.RsaKeyPair;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;

public class CertificateKeysUtilsTest {

    @Test
    public void testGenerateRsaKeyPair() {
        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        assertThat(rsaKeyPair).isNotNull();
        assertThat(rsaKeyPair.privateKeyPem).isNotEmpty();
        assertThat(rsaKeyPair.publicKeyPem).isNotEmpty();
    }

    @Test
    public void testGeneratePrivateKey() {
        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        val privateKey = CertificateKeysUtils.generatePrivateKey(rsaKeyPair.privateKeyPem);
        assertThat(privateKey).isNotNull();
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    }

    @Test
    public void testGeneratePublicKey() {
        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        val publicKey = CertificateKeysUtils.generatePublicKey(rsaKeyPair.publicKeyPem);
        assertThat(publicKey).isNotNull();
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(publicKey.getFormat()).isEqualTo("X.509");
    }
}