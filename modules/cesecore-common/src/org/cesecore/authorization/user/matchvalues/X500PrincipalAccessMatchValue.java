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
package org.cesecore.authorization.user.matchvalues;

import org.cesecore.authentication.tokens.X509CertificateAuthenticationToken;

/**
 * Match with constants. Observe that these constants are also used as a priority indicator for access rules. The higher values the higher priority.
 * 
 * @version $Id: X500PrincipalAccessMatchValue.java 18613 2014-03-17 13:31:40Z mikekushner $
 * 
 */
public enum X500PrincipalAccessMatchValue implements AccessMatchValue {
    NONE(0), WITH_COUNTRY(1), WITH_DOMAINCOMPONENT(2), WITH_STATEORPROVINCE(3), WITH_LOCALITY(4), WITH_ORGANIZATION(5), WITH_ORGANIZATIONALUNIT(6), WITH_TITLE(7), WITH_COMMONNAME(
            8), WITH_UID(9), WITH_DNSERIALNUMBER(10), WITH_SERIALNUMBER(11), WITH_DNEMAILADDRESS(12), WITH_RFC822NAME(13), WITH_UPN(14), WITH_FULLDN(15);
    
    static {
        AccessMatchValueReverseLookupRegistry.INSTANCE.register(X500PrincipalAccessMatchValue.values());
    }
    
    private final int numericValue;
    
    private X500PrincipalAccessMatchValue(int numericValue) {
        this.numericValue = numericValue;
    }

    @Override
    public int getNumericValue() {
        return numericValue;
    }

    @Override
    public boolean isDefaultValue() {
        return numericValue == WITH_SERIALNUMBER.numericValue;
    }

    @Override
    public String getTokenType() {
        return X509CertificateAuthenticationToken.TOKEN_TYPE;
    }

    @Override
    public boolean isIssuedByCa() {
        return true;
    }
}
