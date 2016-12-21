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
package org.cesecore.authentication.tokens;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cesecore.authorization.user.AccessUserAspect;
import org.cesecore.authorization.user.matchvalues.AccessMatchValue;
import org.cesecore.authorization.user.matchvalues.AccessMatchValueReverseLookupRegistry;
import org.cesecore.authorization.user.matchvalues.X500PrincipalAccessMatchValue;
import org.cesecore.certificates.util.DNFieldExtractor;
import org.cesecore.util.CertTools;

/**
 * This is an implementation of the AuthenticationToken concept, based on using an {@link X509Certificate} as it's single credential, and that
 * certificate's {@link X500Principal} as its principle, but as the X500Principle is contained in the X509Certificate, this remains little more than a
 * formality. This AuthenticationToken is the default used in EJBCA.
 * 
 * The implementation of the <code>matches(...)</code> method is based on <code>AdminEntity.java 10832 2010-12-13 13:54:25Z anatom</code> from EJBCA.
 * 
 * 
 * @version $Id: X509CertificateAuthenticationToken.java 17625 2013-09-20 07:12:06Z netmackan $
 * 
 */
public class X509CertificateAuthenticationToken extends LocalJvmOnlyAuthenticationToken {

    public static final String TOKEN_TYPE = "CertificateAuthenticationToken";
    
    private static final Logger log = Logger.getLogger(X509CertificateAuthenticationToken.class);
    
    private static final long serialVersionUID = 1097165653913865515L;

    private static final Pattern serialPattern = Pattern.compile("\\bSERIALNUMBER=", Pattern.CASE_INSENSITIVE);

    private final X509Certificate certificate;

    private final int adminCaId;
    private final DNFieldExtractor dnExtractor;
    private final DNFieldExtractor anExtractor;

    /**
     * Standard constructor for X509CertificateAuthenticationToken
     * 
     * @param principals
     *            A set of X500Principals. Should contain one and only one value.
     * @param credentials
     *            A set of X509Certificates. As with the principals, this set should contain one and only one value, anything else will result in a
     *            {@link InvalidAuthenticationTokenException} being thrown.
     */
    public X509CertificateAuthenticationToken(Set<X500Principal> principals, Set<X509Certificate> credentials) {
        super(principals, credentials);

        /*
         * In order to save having to verify the credentials set every time the <code>matches(...)</code> method is called, it's checked here, and the
         * resulting credential is stored locally.
         */
        X509Certificate[] certificateArray = getCredentials().toArray(new X509Certificate[0]);
        if (certificateArray.length != 1) {
            throw new InvalidAuthenticationTokenException("X509CertificateAuthenticationToken was containing " + certificateArray.length
                    + " credentials instead of 1.");
        } else {
            certificate = certificateArray[0];
        }
        
        String certstring = CertTools.getSubjectDN(certificate).toString();
        adminCaId = CertTools.getIssuerDN(certificate).hashCode();
        certstring = serialPattern.matcher(certstring).replaceAll("SN=");
        String altNameString = CertTools.getSubjectAlternativeName(certificate);

        dnExtractor = new DNFieldExtractor(certstring, DNFieldExtractor.TYPE_SUBJECTDN);
        anExtractor = new DNFieldExtractor(altNameString, DNFieldExtractor.TYPE_SUBJECTALTNAME);
    }

    /**
     * This implementation presumes that a lone {@link X509Certificate} has been submitted as a credential (which should have been verified by the
     * constructor), and will use that value to match this authentication token to the AccessUserData entity submitted.
     * 
     * FIXME: This class is a candidate for optimization. 
     * FIXME: Attempt to remove as many static calls as possible.
     * 
     */
    @Override
    public boolean matches(AccessUserAspect accessUser) {
    	// Protect against spoofing by checking if this token was created locally
    	if (!super.isCreatedInThisJvm()) {
    		return false;
    	}
        boolean returnvalue = false;

        int parameter;
        int size = 0;
        String[] clientstrings = null;
        if (StringUtils.equals(TOKEN_TYPE,accessUser.getTokenType())) {
            // First check that issuers match.
            if (accessUser.getCaId() == adminCaId) {
                // Determine part of certificate to match with.
                DNFieldExtractor usedExtractor = dnExtractor;
                X500PrincipalAccessMatchValue matchValue = (X500PrincipalAccessMatchValue) getMatchValueFromDatabaseValue(accessUser.getMatchWith());
                if (matchValue == X500PrincipalAccessMatchValue.WITH_SERIALNUMBER) {
                    try {
                    BigInteger matchValueAsBigInteger = new BigInteger(accessUser.getMatchValue(), 16);
                        switch (accessUser.getMatchTypeAsType()) {
                        case TYPE_EQUALCASE:
                        case TYPE_EQUALCASEINS:
                            returnvalue = matchValueAsBigInteger.equals(certificate.getSerialNumber());
                            break;
                        case TYPE_NOT_EQUALCASE:
                        case TYPE_NOT_EQUALCASEINS:
                            returnvalue = !matchValueAsBigInteger.equals(certificate.getSerialNumber());
                            break;
                        default:
                        }
                    } catch (NumberFormatException nfe) {
                        log.info("Invalid matchValue for accessUser when expecting a hex serialNumber: "+accessUser.getMatchValue());
                    }
                } else if (matchValue == X500PrincipalAccessMatchValue.WITH_FULLDN) {
                    String value = accessUser.getMatchValue();
                    switch (accessUser.getMatchTypeAsType()) {
                    case TYPE_EQUALCASE:
                        returnvalue = value.equals(CertTools.getSubjectDN(certificate));
                    case TYPE_EQUALCASEINS:
                        returnvalue = value.equalsIgnoreCase(CertTools.getSubjectDN(certificate));
                        break;
                    case TYPE_NOT_EQUALCASE:
                        returnvalue = !value.equals(CertTools.getSubjectDN(certificate));
                    case TYPE_NOT_EQUALCASEINS:
                        returnvalue = !value.equalsIgnoreCase(CertTools.getSubjectDN(certificate));
                        break;
                    default:
                    }
                } else {
                    parameter = DNFieldExtractor.CN;
                    switch (matchValue) {
                    case WITH_COUNTRY:
                        parameter = DNFieldExtractor.C;
                        break;
                    case WITH_DOMAINCOMPONENT:
                        parameter = DNFieldExtractor.DC;
                        break;
                    case WITH_STATEORPROVINCE:
                        parameter = DNFieldExtractor.ST;
                        break;
                    case WITH_LOCALITY:
                        parameter = DNFieldExtractor.L;
                        break;
                    case WITH_ORGANIZATION:
                        parameter = DNFieldExtractor.O;
                        break;
                    case WITH_ORGANIZATIONALUNIT:
                        parameter = DNFieldExtractor.OU;
                        break;
                    case WITH_TITLE:
                        parameter = DNFieldExtractor.T;
                        break;
                    case WITH_DNSERIALNUMBER:
                        parameter = DNFieldExtractor.SN;
                        break;
                    case WITH_COMMONNAME:
                        parameter = DNFieldExtractor.CN;
                        break;
                    case WITH_UID:
                        parameter = DNFieldExtractor.UID;
                        break;
                    case WITH_DNEMAILADDRESS:
                        parameter = DNFieldExtractor.E;
                        break;
                    case WITH_RFC822NAME:
                        parameter = DNFieldExtractor.RFC822NAME;
                        usedExtractor = anExtractor;
                        break;
                    case WITH_UPN:
                        parameter = DNFieldExtractor.UPN;
                        usedExtractor = anExtractor;
                        break;
                    default:
                    }
                    size = usedExtractor.getNumberOfFields(parameter);
                    clientstrings = new String[size];
                    for (int i = 0; i < size; i++) {
                        clientstrings[i] = usedExtractor.getField(parameter, i);
                    }

                    // Determine how to match.
                    if (clientstrings != null) {
                        switch (accessUser.getMatchTypeAsType()) {
                        case TYPE_EQUALCASE:
                            for (int i = 0; i < size; i++) {
                                returnvalue = clientstrings[i].equals(accessUser.getMatchValue());
                                if (returnvalue) {
                                    break;
                                }
                            }
                            break;
                        case TYPE_EQUALCASEINS:
                            for (int i = 0; i < size; i++) {
                                returnvalue = clientstrings[i].equalsIgnoreCase(accessUser.getMatchValue());
                                if (returnvalue) {
                                    break;
                                }
                            }
                            break;
                        case TYPE_NOT_EQUALCASE:
                            for (int i = 0; i < size; i++) {
                                returnvalue = !clientstrings[i].equals(accessUser.getMatchValue());
                                if (returnvalue) {
                                    break;
                                }
                            }
                            break;
                        case TYPE_NOT_EQUALCASEINS:
                            for (int i = 0; i < size; i++) {
                                returnvalue = !clientstrings[i].equalsIgnoreCase(accessUser.getMatchValue());
                                if (returnvalue) {
                                    break;
                                }
                            }
                            break;
                        default:
                        }
                    }
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Caid does not match. Required="+adminCaId+", actual was "+accessUser.getCaId());
                }
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Token type does not match. Required="+TOKEN_TYPE+", actual was "+accessUser.getTokenType());
            }            
        }

        return returnvalue;
    }

    /** Returns user information of the user this authentication token belongs to. */
    @Override
    public String toString() {
    	return CertTools.getSubjectDN(certificate);
    }

    @Override
    public int hashCode() {
        final int prime = 4711;
        int result = 1;
        result = prime * result + ((certificate == null) ? 0 : certificate.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        X509CertificateAuthenticationToken other = (X509CertificateAuthenticationToken) obj;
        if (certificate == null) {
            if (other.certificate != null) {
                return false;
            }
        } else if (!certificate.equals(other.certificate)) {
            return false;
        }
        return true;
    }

    /**
     * @return the certificate
     */
    public X509Certificate getCertificate() {
        return certificate;
    }
    
    @Override
    public boolean matchTokenType(String tokenType) {
        return tokenType.equals(TOKEN_TYPE);
    }
    
    @Override
    public AccessMatchValue getMatchValueFromDatabaseValue(Integer databaseValue) {
        return AccessMatchValueReverseLookupRegistry.INSTANCE.performReverseLookup(TOKEN_TYPE, databaseValue.intValue());
    }
    
    @Override
    public AccessMatchValue getDefaultMatchValue() {        
        return X500PrincipalAccessMatchValue.NONE;
    }
}

