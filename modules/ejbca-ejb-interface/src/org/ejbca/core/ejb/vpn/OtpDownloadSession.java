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

import org.cesecore.vpn.OtpDownload;
import org.cesecore.vpn.VpnUser;

import java.util.List;

/**
 * Basic CRUD operations on OtpDownload.
 *
 * @author ph4r05
 */
public interface OtpDownloadSession {

    /**
     * @return the specified Otp or null if it does not exist.
     * @throws RuntimeException
     */
    OtpDownload getOtp(int otpId);

    /**
     * @return the specified Otp or null if it does not exist.
     * @throws RuntimeException
     */
    List<OtpDownload> getOtp(String otpId);

    /**
     * @return the specified Otp or null if it does not exist.
     * @throws RuntimeException
     */
    List<OtpDownload> getOtp(String otpType, String otpId);

    /**
     * Add the specified VPNUser to the database and return the id used to store it
     * @param otpDownload
     * @return
     */
    OtpDownload merge(OtpDownload otpDownload);

    /**
     * Remove the specified OTP Download from the database.
     * @param id the id of the otp record to remove
     * @return true if crypto token exists and is deleted, false if crypto token with given id does not exist
     */
    boolean remove(final int id);

    /**
     * Loads OtpDownload via OTP token. If token matches, user is returned.
     * Configuration is not changed.
     *
     * @param otpToken OTP token
     * @return VpnUser is returned, attached to persistence context
     */
    OtpDownload downloadOtp(String otpToken);

    /**
     * Returns true if the token is already taken.
     * @param otpType
     * @param otpId
     * @param otpResource
     * @return
     */
    boolean isOtpTokenTaken(final String otpType, final String otpId, final String otpResource);
}
