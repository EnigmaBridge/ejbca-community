package org.ejbca.core.ejb.vpn;

import org.cesecore.ErrorCode;

/**
 * OTP error - OTP token is too old.
 *
 * Created by dusanklinec on 19.01.17.
 */
public class VpnOtpOldException extends VpnOtpException {
    public VpnOtpOldException() {
    }

    public VpnOtpOldException(String message) {
        super(message);
    }

    public VpnOtpOldException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VpnOtpOldException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public VpnOtpOldException(Exception exception) {
        super(exception);
    }

    public VpnOtpOldException(ErrorCode errorCode, Exception exception) {
        super(errorCode, exception);
    }

    public VpnOtpOldException(String message, Throwable cause) {
        super(message, cause);
    }

    public VpnOtpOldException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
