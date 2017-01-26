package org.ejbca.ui.web.pub.vpn;

/**
 * OTP Link error.
 *
 * @author ph4r05
 * Created by dusanklinec on 26.01.17.
 */
public enum VpnLinkError {
    NONE,
    OTP_INVALID,
    OTP_OLD,
    OTP_TOO_MANY,
    OTP_COOKIE,
    OTP_DESCRIPTOR,
    NO_CONFIGURATION,
    GENERIC
}
