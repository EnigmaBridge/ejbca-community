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
import org.cesecore.vpn.VpnUser;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * VpnUser management operations that require authorization and/or security events audit logging.
 *
 * @author ph4r05
 */
public interface VpnUserManagementSession {
    /**
     * Fetches list of VPN users.
     *
     * @param authenticationToken auth token
     * @return a list of IDs for VpnUsers that the caller is authorized to view
     */
    List<Integer> geVpnUsersIds(AuthenticationToken authenticationToken);

    /**
     * Returns true if the given user name is available
     * @param user vpn user
     * @return true if available and VpnUser can be stored
     */
    boolean isUsernameAvailable(AuthenticationToken authenticationToken, VpnUser user) throws AuthorizationDeniedException;

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
     * Loads VpnUser from the database using the email + device.
     * @param authenticationToken auth token
     * @param email VPN user email
     * @param device VPN user device
     * @return VPN user
     * @throws AuthorizationDeniedException token invalid
     */
    VpnUser getVpnUser(AuthenticationToken authenticationToken, String email, String device) throws AuthorizationDeniedException;

    /**
     * Checks if OTP for VPN config download is valid.
     * Returns detached VpnUser copy detached from persistence context, with sensitive fields removed.
     * Does not modify VpnUser like downloadOtp() call. If OTP is invalid, sensitive data is cleared.
     *
     * @param authenticationToken token invalid
     * @param vpnUserId vpn user id
     * @param otpToken OTP for download
     * @param properties user identification and misc data
     * @return Vpnser copy, with nulled sensitive data
     * @throws VpnOtpInvalidException OTP is invalid
     * @throws VpnOtpTooManyException OTP used too many times
     * @throws VpnOtpOldException OTP is too old to use
     * @throws VpnNoConfigException VPN configuration is empty
     * @throws VpnOtpDescriptorException OTP descriptor does not match, different device & source fingerprint as required
     */
    VpnUser checkOtp(AuthenticationToken authenticationToken, int vpnUserId, String otpToken, Properties properties) throws VpnOtpInvalidException, VpnOtpTooManyException, VpnOtpOldException, VpnNoConfigException, VpnOtpDescriptorException;

    /**
     * Loads VpnUser via OTP token. If token matches and multiple criteria are met user is returned.
     * If check criteria are invalid a corresponding exception is thrown.
     *
     * @param authenticationToken auth token
     * @param vpnUserId VPN user id
     * @param otpToken OTP token
     * @param cookie OTP cookie - if downloaded previously
     * @param properties user identification and misc data
     * @return vpn user
     *
     * @throws AuthorizationDeniedException auth token invalid
     * @throws VpnOtpOldException OTP is too old to use
     * @throws VpnOtpTooManyException OTP used too many times
     * @throws VpnOtpCookieException Cookie is invalid, does not match required cookie
     * @throws VpnOtpDescriptorException OTP descriptor does not match, different device & source fingerprint as required
     * @throws VpnOtpInvalidException OTP is invalid
     * @throws VpnNoConfigException VPN configuration is empty
     */
    VpnUser downloadOtp(AuthenticationToken authenticationToken, int vpnUserId, String otpToken, String cookie, Properties properties)
            throws AuthorizationDeniedException, VpnOtpOldException, VpnOtpTooManyException, VpnOtpCookieException, VpnOtpDescriptorException, VpnOtpInvalidException, VpnNoConfigException;

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
     * The user state is changed accordingly, resulting VpnUser after merge is returned.
     *
     * @param authenticationToken auth token
     * @param vpnUserId user entity ID
     * @param password password to use, optional. If not set, the one from entity is used.
     * @param properties optional properties
     * @throws AuthorizationDeniedException token invalid
     * @throws CADoesntExistsException invalid CA in the end entity
     * @throws IOException IO exception in key gen / templates
     * @throws VpnException Generic exception encapsulating many internal exceptions
     *      (e.g., UserDoesntFullfillEndEntityProfile)
     */
    VpnUser newVpnCredentials(AuthenticationToken authenticationToken, int vpnUserId, OptionalNull<String> password, Properties properties)
            throws AuthorizationDeniedException, CADoesntExistsException, IOException, VpnException;

    /**
     * Generates a new VPN CRL.
     *
     * @param authenticationToken auth token
     * @param force if true the new CRL is generated no matter what
     * @param overlapMilli if force is false, this defines the time in milliseconds the new CRL is generated before the old one expires.
     * @return number of the CRL ID generated, null if CRL was not generated
     * @throws AuthorizationDeniedException token invalid
     * @throws CADoesntExistsException VPN ca does not exist
     * @throws VpnException Generic exception encapsulating many internal exceptions
     */
    Integer generateCRL(AuthenticationToken authenticationToken, boolean force, Long overlapMilli) throws AuthorizationDeniedException, CADoesntExistsException, VpnException;

    /**
     * Returns the latest CRL generated by the VPN CA.
     *
     * @param authenticationToken auth token
     * @return byte[] with DER encoded X509CRL or null of no CRLs have been issued.
     * @throws AuthorizationDeniedException token invalid
     * @throws CADoesntExistsException VPN ca does not exist
     * @throws VpnException Generic exception encapsulating many internal exceptions
     */
    byte[] getCRL(AuthenticationToken authenticationToken) throws AuthorizationDeniedException, CADoesntExistsException, VpnException;
}
