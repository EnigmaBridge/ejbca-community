package org.ejbca.core.ejb.vpn;

import org.cesecore.ErrorCode;

/**
 * OTP error - OTP token downloaded too many times.
 *
 * Created by dusanklinec on 19.01.17.
 */
public class VpnOtpTooManyException extends VpnOtpException {
    public VpnOtpTooManyException() {
    }

    public VpnOtpTooManyException(String message) {
        super(message);
    }

    public VpnOtpTooManyException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VpnOtpTooManyException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public VpnOtpTooManyException(Exception exception) {
        super(exception);
    }

    public VpnOtpTooManyException(ErrorCode errorCode, Exception exception) {
        super(errorCode, exception);
    }

    public VpnOtpTooManyException(String message, Throwable cause) {
        super(message, cause);
    }

    public VpnOtpTooManyException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
