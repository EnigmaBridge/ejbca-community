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
package org.cesecore.vpn;

import java.util.List;

/**
 * @version $Id: VpnUserSession.java 19902 2014-09-30 14:32:24Z anatom $
 *
 */
public interface VpnUserSession {

    /** @return true if the specified name is already in use by another VpnUser (checks the database, not the cache) */
    boolean isVpnUserNameUsed(String email);
    boolean isVpnUserNameUsed(String email, String device);

    /** @return the specified CryptoToken or null if it does not exis.
     * @throws RuntimeException  */
    VpnUser getVpnUser(int vpnUserId);

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

    /** Add the specified CryptoToken to the database and return the id used to store it */
    VpnUser mergeVpnUser(VpnUser vpnUser) throws VpnUserNameInUseException;

    /** Remove the specified CryptoToken from the database.
     * @param vpnUserId the id of the crypto token that should be removed
     * @return true if crypto token exists and is deleted, false if crypto token with given id does not exist
     */
    boolean removeVpnUser(final int vpnUserId);

    /** @return a list of all CryptoToken identifiers in the database. */
    List<Integer> getVpnUserIds();

    /** Clears the CryptoToken cache. */
    void flushCache();

    /** Clears the CryptoToken cache except for the cache entries specified in excludeIDs */
    void flushExcludingIDs(List<Integer> excludeIDs);
}
