package com.authbox.base.util;

import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.RsaKeyPair;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static java.util.UUID.randomUUID;

public final class CertificateKeysUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateKeysUtils.class);
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final int BITS = 2048;
    private static final String ALGORITHM = "RSA";

    static {
        java.security.Security.addProvider(new BouncyCastleProvider());
    }

    private CertificateKeysUtils() {
        throw new IllegalStateException("Use static methods directly, without using constructor");
    }

    public static RsaKeyPair generateRsaKeyPair() {
        Path filenamePrivate = null;
        Path filenamePublic = null;
        try {
            filenamePrivate = Path.of(TEMP_DIR + "/" + randomUUID().toString() + "_private.pem");
            filenamePublic = Path.of(TEMP_DIR + "/" + randomUUID().toString() + "_public.pem");
            ExecUtils.executeCommand("openssl genrsa -out " + filenamePrivate + " " + BITS);
            ExecUtils.executeCommand("openssl rsa -in " + filenamePrivate + " -outform PEM -pubout -out " + filenamePublic);
            return new RsaKeyPair(
                    Files.readString(filenamePrivate),
                    Files.readString(filenamePublic)
            );
        } catch (final IOException | InterruptedException e) {
            LOGGER.error("Unable to generate rsa key pair: {}", e.getMessage(), e);
            throw new BadRequestException("Unable to generate rsa key pair: " + e.getMessage());
        } finally {
            if (filenamePrivate != null) {
                filenamePrivate.toFile().delete();
            }
            if (filenamePublic != null) {
                filenamePublic.toFile().delete();
            }
        }
    }

    public static PublicKey generatePublicKey(final String pem) throws IllegalArgumentException {
        try {
            final String publicKeyPem = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("-----END RSA PUBLIC KEY-----", "");
            ;
            final byte[] encoded = Base64.decodeBase64(publicKeyPem);
            final KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return keyFactory.generatePublic(keySpec);
        } catch (final Exception e) {
            LOGGER.error("Unable to create public key: {}", e.getMessage());
            throw new IllegalArgumentException("Unable to create public key: " + e.getMessage());
        }
    }

    public static PrivateKey generatePrivateKey(final String pem) throws IllegalArgumentException {
        try {
            final String privateKeyPem = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "");
            final byte[] encoded = Base64.decodeBase64(privateKeyPem);
            final KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return keyFactory.generatePrivate(keySpec);
        } catch (final Exception e) {
            LOGGER.error("Unable to create private key: {}", e.getMessage());
            throw new IllegalArgumentException("Unable to create private key: " + e.getMessage());
        }
    }
}
