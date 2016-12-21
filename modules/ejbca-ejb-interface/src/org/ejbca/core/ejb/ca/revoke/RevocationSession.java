/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.ejb.ca.revoke;

import java.math.BigInteger;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Date;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.certificate.CertificateRevokeException;


/**
 * Used for evoking certificates in the system, manages revocation by:
 * - Setting revocation status in the database (using certificate store)
 * - Publishing revocations to publishers 
 * 
 * @version $Id: RevocationSession.java 19902 2014-09-30 14:32:24Z anatom $
 */
public interface RevocationSession {
	
	/** @see #revokeCertificate(AuthenticationToken, Certificate, Collection, int, String)
	 * 
     * @param admin      Administrator performing the operation
	 * @param issuerdn certificate issuerDN
	 * @param serno certificate serial number
	 * @param revokedate a specific revocation date
     * @param publishers and array of publisher ids (Integer) of publishers to revoke the certificate in.
     * @param reason     the reason of the revocation. (One of the RevokedCertInfo.REVOCATION_REASON constants.)
     * @param userDataDN if an DN object is not found in the certificate use object from user data instead.
     * @throws CertificaterevokeException (rollback) if certificate does not exist
     * @throws AuthorizationDeniedException (rollback)
	 */
    public void revokeCertificate(AuthenticationToken admin, String issuerdn, BigInteger serno, Date revokedate, Collection<Integer> publishers, int reason, String userDataDN) throws CertificateRevokeException, AuthorizationDeniedException;

    /**
     * Revokes a certificate, in the database and in publishers. Also handles re-activation of suspended certificates.
     *
     * Re-activating (unrevoking) a certificate have two limitations.
     * 1. A password (for for example AD) will not be restored if deleted, only the certificate and certificate status and associated info will be restored
     * 2. ExtendedInformation, if used by a publisher will not be used when re-activating a certificate 
     * 
     * The method leaves up to the caller to find the correct publishers and userDataDN.
     *
     * @param admin      Administrator performing the operation
     * @param cert       The DER coded Certificate that has been revoked.
     * @param publishers and array of publisher ids (Integer) of publishers to revoke the certificate in.
     * @param reason     the reason of the revocation. (One of the RevokedCertInfo.REVOCATION_REASON constants.)
     * @param userDataDN if an DN object is not found in the certificate use object from user data instead.
     * @throws CertificaterevokeException (rollback) if certificate does not exist
     * @throws AuthorizationDeniedException (rollback)
     */
    void revokeCertificate(AuthenticationToken admin, Certificate cert, Collection<Integer> publishers, int reason, String userDataDN) throws CertificateRevokeException, AuthorizationDeniedException;

}
