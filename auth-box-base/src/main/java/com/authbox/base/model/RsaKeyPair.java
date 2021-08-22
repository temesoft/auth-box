package com.authbox.base.model;

import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class RsaKeyPair {

    public final String privateKeyPem;
    public final String publicKeyPem;

}
