package org.ejbca.core.ejb.vpn;

import org.cesecore.ErrorCode;

/**
 * OTP error - cookie does not match
 *
 * @author ph4r05
 * Created by dusanklinec on 19.01.17.
 */
public class VpnOtpCookieException extends VpnOtpException {
    public VpnOtpCookieException() {
    }

    public VpnOtpCookieException(String message) {
        super(message);
    }

    public VpnOtpCookieException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VpnOtpCookieException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public VpnOtpCookieException(Exception exception) {
        super(exception);
    }

    public VpnOtpCookieException(ErrorCode errorCode, Exception exception) {
        super(errorCode, exception);
    }

    public VpnOtpCookieException(String message, Throwable cause) {
        super(message, cause);
    }

    public VpnOtpCookieException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
