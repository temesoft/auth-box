package com.authbox.base.model;

public class RsaKeyPair {

    public final String privateKeyPem;
    public final String publicKeyPem;

    public RsaKeyPair(final String privateKeyPem, final String publicKeyPem) {
        this.privateKeyPem = privateKeyPem;
        this.publicKeyPem = publicKeyPem;
    }
}
