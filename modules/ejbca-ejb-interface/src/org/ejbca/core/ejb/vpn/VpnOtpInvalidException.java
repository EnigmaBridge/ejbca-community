package org.ejbca.core.ejb.vpn;

import org.cesecore.ErrorCode;

/**
 * OTP was not valid.
 *
 * @author ph4r05
 * Created by dusanklinec on 19.01.17.
 */
public class VpnOtpInvalidException extends VpnOtpException {
    public VpnOtpInvalidException() {
    }

    public VpnOtpInvalidException(String message) {
        super(message);
    }

    public VpnOtpInvalidException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VpnOtpInvalidException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public VpnOtpInvalidException(Exception exception) {
        super(exception);
    }

    public VpnOtpInvalidException(ErrorCode errorCode, Exception exception) {
        super(errorCode, exception);
    }

    public VpnOtpInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public VpnOtpInvalidException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
