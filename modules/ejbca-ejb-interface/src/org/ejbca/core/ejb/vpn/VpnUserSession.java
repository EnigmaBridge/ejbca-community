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
package org.ejbca.core.ejb.vpn;

import org.cesecore.vpn.VpnUser;

import java.util.List;

/**
 * Basic CRUD operations on VpnUser.
 *
 * @author ph4r05
 */
public interface VpnUserSession {

    /**
     *
     * @param email
     * @return true if the specified name is already in use by another VpnUser (checks the database, not the cache)
     */
    boolean isVpnUserNameUsed(String email);
    boolean isVpnUserNameUsed(String email, String device);

    /**
     * @return the specified VPNUser or null if it does not exist.
     * @throws RuntimeException
     */
    VpnUser getVpnUser(int vpnUserId);

    /**
     * Returns all vpn user records for the given user email
     * @param ids identifiers
     * @return
     */
    List<VpnUser> getVpnUsers(final List<Integer> ids);

    /**
     * Returns all vpn user records for the given user email
     * @param email
     * @return
     */
    List<VpnUser> getVpnUser(final String email);

    /**
     * Returns single user record for (email, device) ID.
     * @param email
     * @param device
     * @return
     */
    VpnUser getVpnUser(String email, String device);

    /**
     * Add the specified VPNUser to the database and return the id used to store it
     * @param vpnUser
     * @return
     * @throws VpnUserNameInUseException
     */
    VpnUser mergeVpnUser(VpnUser vpnUser) throws VpnUserNameInUseException;

    /** Remove the specified VPNUser from the database.
     * @param vpnUserId the id of the crypto token that should be removed
     * @return true if crypto token exists and is deleted, false if crypto token with given id does not exist
     */
    boolean removeVpnUser(final int vpnUserId);

    /**
     * Revokes VPNUser - deletes all crypto related data.
     * @param vpnUserId
     * @return
     */
    boolean revokeVpnUser(final int vpnUserId);

    /**
     * Loads VpnUser via OTP token. If token matches, user is returned.
     * Configuration is not changed.
     *
     * @param vpnUserId vpn user id
     * @param otpToken OTP token
     * @return VpnUser is returned, attached to persistence context
     */
    VpnUser downloadOtp(int vpnUserId, String otpToken);

    /**
     * 
     * @return a list of all VPNUser identifiers in the database.
     */
    List<Integer> getVpnUserIds();

    /**
     * Clears the VPNUser cache.
     */
    void flushCache();

    /**
     * Clears the VPNUser cache except for the cache entries specified in excludeIDs
     * @param excludeIDs
     */
    void flushExcludingIDs(List<Integer> excludeIDs);
}
