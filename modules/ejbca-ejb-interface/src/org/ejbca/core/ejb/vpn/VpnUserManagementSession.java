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
package org.ejbca.core.ejb.vpn;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.vpn.VpnUser;

import javax.ejb.Local;
import java.io.IOException;
import java.security.KeyStore;
import java.util.List;
import java.util.Properties;

/**
 * VpnUser management operations that require authorization and/or security events audit logging.
 *
 * @author ph4r05
 */
@Local
public interface VpnUserManagementSession {
    /**
     * Fetches list of VPN users.
     *
     * @param authenticationToken auth token
     * @return a list of IDs for VpnUsers that the caller is authorized to view
     */
    List<Integer> geVpnUsersIds(AuthenticationToken authenticationToken);

    /**
     * Returns end entity user name generated from the VPN record.
     * @param user VPN user entity
     * @return unique VPN user identifier (email + device).
     */
    String getUserName(VpnUser user);

    /**
     * Deletes only the VPN user record, preserves certificate and end entity record.
     * @param authenticationToken
     * @param vpnUserId
     * @throws AuthorizationDeniedException
     */
    void deleteVpnUser(AuthenticationToken authenticationToken, int vpnUserId) throws AuthorizationDeniedException;

    /**
     * Revokes the VPN user - deletes all certificate related data from the VPN DB.
     * @param authenticationToken auth token
     * @param vpnUserId VPN user id
     * @throws AuthorizationDeniedException token invalid
     */
    void revokeVpnUser(AuthenticationToken authenticationToken, int vpnUserId) throws AuthorizationDeniedException;

    /**
     * Loads VpnUser from the database using ID.
     * @param authenticationToken auth token
     * @param vpnUserId VPN user id
     * @return VPN user
     * @throws AuthorizationDeniedException token invalid
     */
    VpnUser getVpnUser(AuthenticationToken authenticationToken, int vpnUserId) throws AuthorizationDeniedException;

    /**
     * Loads VpnUser via OTP token. If token matches, user is returned
     * and VPN config is removed from database.
     *
     * @param authenticationToken auth token
     * @param vpnUserId VPN user id
     * @param otpToken OTP token
     * @return vpn user
     * @throws AuthorizationDeniedException token invalid
     */
    VpnUser downloadOtp(AuthenticationToken authenticationToken, int vpnUserId, String otpToken, Properties properties)
            throws AuthorizationDeniedException;

    VpnUser createVpnUser(final AuthenticationToken authenticationToken, VpnUser user)
            throws AuthorizationDeniedException, VpnUserNameInUseException;

    VpnUser saveVpnUser(AuthenticationToken authenticationToken, VpnUser user)
            throws AuthorizationDeniedException, VpnUserNameInUseException;

    /**
     * Sends a configuration email or throws an exception
     * @param authenticationToken auth token
     * @param vpnUserId vpn user ID
     * @param properties optional properties
     * @throws AuthorizationDeniedException token invalid
     * @throws IOException generic problem with IO - email template / email sending
     * @throws VpnMailSendException generic problem with IO - email template / email sending
     */
    void sendConfigurationEmail(AuthenticationToken authenticationToken, int vpnUserId, Properties properties)
            throws AuthorizationDeniedException, VpnMailSendException, IOException;

    /**
     * Generates new VPN credentials - new certificate, VPN configuration. Resets OTP state.
     * @param authenticationToken auth token
     * @param endEntity user end entity
     * @param user VPN user DB entity
     * @throws AuthorizationDeniedException token invalid
     * @throws CADoesntExistsException invalid CA in the end entity
     * @throws IOException IO exception in key gen / templates
     * @throws VpnException Generic exception encapsulating many internal exceptions
     *      (e.g., UserDoesntFullfillEndEntityProfile)
     */
    void newVpnCredentials(AuthenticationToken authenticationToken, EndEntityInformation endEntity, VpnUser user)
            throws AuthorizationDeniedException, CADoesntExistsException, IOException, VpnException;
}
