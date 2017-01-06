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
package org.cesecore.vpn;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.endentity.EndEntityInformation;

import javax.ejb.Local;
import java.security.KeyStore;
import java.util.List;

/**
 * CryptoToken management operations that require authorization and/or security events audit logging.
 * 
 * @version $Id: VpnUserManagementSession.java 20728 2015-02-20 14:55:55Z mikekushner $
 */
@Local
public interface VpnUserManagementSession {
    /** @return a list of IDs for VpnUsers that the caller is authorized to view */
    List<Integer> geVpnUsersIds(AuthenticationToken authenticationToken);

    /**
     * Returns end entity user name generated from the VPN record.
     * @param user
     * @return
     */
    String getUserName(VpnUser user);

    void deleteVpnUser(AuthenticationToken authenticationToken, int vpnUserId) throws AuthorizationDeniedException;

    VpnUser getVpnUser(AuthenticationToken authenticationToken, int vpnUserId) throws AuthorizationDeniedException;

    VpnUser createVpnUser(final AuthenticationToken authenticationToken, VpnUser user)
            throws AuthorizationDeniedException, VpnUserNameInUseException;

    VpnUser saveVpnUser(AuthenticationToken authenticationToken, VpnUser user)
            throws AuthorizationDeniedException, VpnUserNameInUseException;

    String generateVpnConfig(AuthenticationToken authenticationToken, EndEntityInformation user, KeyStore ks)
            throws AuthorizationDeniedException, CADoesntExistsException;
}
