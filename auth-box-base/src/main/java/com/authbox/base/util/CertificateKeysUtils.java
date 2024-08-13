package com.authbox.base.util;

import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.RsaKeyPair;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static com.authbox.base.util.IdUtils.createId;
import static org.apache.commons.codec.binary.Base64.decodeBase64;

@Slf4j
public final class CertificateKeysUtils {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final int BITS = 2048;
    private static final String ALGORITHM = "RSA";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private CertificateKeysUtils() {
        throw new IllegalStateException("Use static methods directly, without using constructor");
    }

    public static RsaKeyPair generateRsaKeyPair() {
        Path filenamePrivate = null;
        Path filenamePublic = null;
        try {
            filenamePrivate = Path.of(TEMP_DIR + "/" + createId() + "_private.pem");
            filenamePublic = Path.of(TEMP_DIR + "/" + createId() + "_public.pem");
            ExecUtils.executeCommand("openssl genrsa -out " + filenamePrivate + " " + BITS);
            ExecUtils.executeCommand("openssl rsa -in " + filenamePrivate + " -outform PEM -pubout -out " + filenamePublic);
            return new RsaKeyPair(
                    Files.readString(filenamePrivate),
                    Files.readString(filenamePublic)
            );
        } catch (final IOException | InterruptedException e) {
            log.error("Unable to generate rsa key pair: {}", e.getMessage(), e);
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
            val publicKeyPem = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("-----END RSA PUBLIC KEY-----", "");
            val encoded = decodeBase64(publicKeyPem);
            val keyFactory = KeyFactory.getInstance(ALGORITHM);
            val keySpec = new X509EncodedKeySpec(encoded);
            return keyFactory.generatePublic(keySpec);
        } catch (final Exception e) {
            log.error("Unable to create public key: {}", e.getMessage());
            throw new IllegalArgumentException("Unable to create public key: " + e.getMessage());
        }
    }

    public static PrivateKey generatePrivateKey(final String pem) throws IllegalArgumentException {
        try {
            val privateKeyPem = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "");
            val encoded = decodeBase64(privateKeyPem);
            val keyFactory = KeyFactory.getInstance(ALGORITHM);
            val keySpec = new PKCS8EncodedKeySpec(encoded);
            return keyFactory.generatePrivate(keySpec);
        } catch (final Exception e) {
            log.error("Unable to create private key: {}", e.getMessage());
            throw new IllegalArgumentException("Unable to create private key: " + e.getMessage());
        }
    }
}
