/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.certificates.ca.catoken;

/**
 * Purpose mapping constants to for CryptoToken aliases and related.
 * 
 * @version $Id: CATokenConstants.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public final class CATokenConstants {

	/** signs certificates issued by the CA */
	public static final int CAKEYPURPOSE_CERTSIGN            = 1;
	/** igns CRLs issues by the CA */
	public static final int CAKEYPURPOSE_CRLSIGN             = 2;
	/** encrypts entity keys stored in the database for key recovery */
	public static final int CAKEYPURPOSE_KEYENCRYPT          = 3;
	/** used for testing if the CA token is functioning and on-line */
	public static final int CAKEYPURPOSE_KEYTEST             = 4;
	/** encrypts hard token PIN/PUK codes etc */
	public static final int CAKEYPURPOSE_HARDTOKENENCRYPT    = 5;
	/** The CAs previous signing key, if any exists */
	public static final int CAKEYPURPOSE_CERTSIGN_PREVIOUS   = 6;
	/** The CAs next signing key, if any exists */
	public static final int CAKEYPURPOSE_CERTSIGN_NEXT       = 7;
	
	/** Key strings for token properties matching the above constants, i.e. when doing getPrivateKey(1)
	 * on a CAToken it will try to use the key configured with label certSignKey in the CryptoToken properties. */
	final static public String CAKEYPURPOSE_CERTSIGN_STRING = "certSignKey";
	final static public String CAKEYPURPOSE_CRLSIGN_STRING = "crlSignKey";
	final static public String CAKEYPURPOSE_KEYENCRYPT_STRING = "keyEncryptKey";
	final static public String CAKEYPURPOSE_TESTKEY_STRING = "testKey";
	final static public String CAKEYPURPOSE_DEFAULT_STRING = "defaultKey";
	final static public String CAKEYPURPOSE_HARDTOKENENCRYPT_STRING = "hardTokenEncrypt";
	final static public String CAKEYPURPOSE_CERTSIGN_STRING_PREVIOUS = "previousCertSignKey";
	final static public String CAKEYPURPOSE_CERTSIGN_STRING_NEXT = "nextCertSignKey";

    /** Previous sequence (matching CryptoTokenConstants.CAKEYPURPOSE_CERTSIGN_STRING_PREVIOUS key) that can be set in CA token properties */
    public static final String PREVIOUS_SEQUENCE_PROPERTY = "previousSequence";
    /** Next sequence (matching CryptoTokenConstants.CAKEYPURPOSE_CERTSIGN_STRING_NEXT key) that can be set in CA token properties */
    public static final String NEXT_SEQUENCE_PROPERTY = "nextSequence";
}
