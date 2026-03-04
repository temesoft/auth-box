package com.authbox.base.util;

import com.authbox.base.model.RsaKeyPair;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

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

/**
 * Utility class for RSA cryptographic key operations.
 * <p>
 * Provides functionality to generate RSA key pairs using system-level OpenSSL commands
 * and convert PEM-encoded strings into {@link PublicKey} and {@link PrivateKey} objects.
 * Uses BouncyCastle as the underlying security provider.
 */
@Slf4j
public final class CertificateKeysUtils {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final int BITS = 2048;
    private static final String ALGORITHM = "RSA";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws IllegalStateException if called.
     */
    private CertificateKeysUtils() {
        throw new IllegalStateException("Use static methods directly, without using constructor");
    }

    /**
     * Generates a new 2048-bit RSA key pair by invoking the system OpenSSL binary.
     * <p>
     * Temporary files are created in the system's temp directory and deleted
     * immediately after the keys are loaded into memory.
     *
     * @return An {@link RsaKeyPair} containing the PEM-encoded private and public keys.
     * @throws IllegalStateException if OpenSSL execution fails or file I/O occurs.
     */
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
            throw new IllegalStateException("Unable to generate rsa key pair: " + e.getMessage());
        } finally {
            if (filenamePrivate != null) {
                val unused = filenamePrivate.toFile().delete();
            }
            if (filenamePublic != null) {
                val unused = filenamePublic.toFile().delete();
            }
        }
    }

    /**
     * Converts a PEM-encoded string into a {@link PublicKey}.
     * <p>
     * This method strips standard RSA headers and footers, removes line separators,
     * and decodes the Base64 content using an X509 specification.
     *
     * @param pem The PEM-encoded public key string.
     * @return The reconstructed {@link PublicKey}.
     * @throws IllegalArgumentException if the PEM string is malformed or invalid.
     */
    public static PublicKey generatePublicKey(final String pem) throws IllegalArgumentException {
        try {
            val publicKeyPem = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("-----END RSA PUBLIC KEY-----", "");
            val encoded = Base64.decode(publicKeyPem);
            val keyFactory = KeyFactory.getInstance(ALGORITHM);
            val keySpec = new X509EncodedKeySpec(encoded);
            return keyFactory.generatePublic(keySpec);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Unable to create public key: " + e.getMessage());
        }
    }

    /**
     * Converts a PEM-encoded string into a {@link PrivateKey}.
     * <p>
     * This method strips standard RSA headers and footers, removes line separators,
     * and decodes the Base64 content using a PKCS8 specification.
     *
     * @param pem The PEM-encoded private key string.
     * @return The reconstructed {@link PrivateKey}.
     * @throws IllegalArgumentException if the PEM string is malformed or invalid.
     */
    public static PrivateKey generatePrivateKey(final String pem) throws IllegalArgumentException {
        try {
            val privateKeyPem = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "");
            val encoded = Base64.decode(privateKeyPem);
            val keyFactory = KeyFactory.getInstance(ALGORITHM);
            val keySpec = new PKCS8EncodedKeySpec(encoded);
            return keyFactory.generatePrivate(keySpec);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Unable to create private key: " + e.getMessage());
        }
    }
}
