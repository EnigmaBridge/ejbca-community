/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.keys.util;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Wrapper class for serializing PublicKey objects. 
 * 
 * @version $Id: PublicKeyWrapper.java 20794 2015-03-02 12:38:57Z mikekushner $
 *
 */
public class PublicKeyWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    final byte[] encodedKey;
    final String algorithm;
    transient PublicKey publicKey;

    public PublicKeyWrapper(final PublicKey publicKey) {
        this.encodedKey = publicKey.getEncoded();
        this.algorithm = publicKey.getAlgorithm();
    }

    /**
     * 
     * @return the decoded PublicKey object wrapped in this class.
     * 
     */
    public PublicKey getPublicKey() {
        if (publicKey == null) {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance(algorithm, BouncyCastleProvider.PROVIDER_NAME);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
                publicKey = keyFactory.generatePublic(keySpec);
            } catch (NoSuchProviderException e) {
                throw new IllegalStateException("BouncyCastle was not a known provider.", e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Algorithm " + algorithm + " was not known at deserialisation", e);
            } catch (InvalidKeySpecException e) {
                throw new IllegalStateException("The incorrect key specification was implemented.", e);
            }
        }
        return publicKey;
    }

}
