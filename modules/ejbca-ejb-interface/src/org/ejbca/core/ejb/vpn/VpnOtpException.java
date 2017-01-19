package org.ejbca.core.ejb.vpn;

import org.cesecore.ErrorCode;

/**
 * General exception related to OTP.
 *
 * @author ph4r05
 * Created by dusanklinec on 19.01.17.
 */
public class VpnOtpException extends VpnException {
    public VpnOtpException() {
    }

    public VpnOtpException(String message) {
        super(message);
    }

    public VpnOtpException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VpnOtpException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public VpnOtpException(Exception exception) {
        super(exception);
    }

    public VpnOtpException(ErrorCode errorCode, Exception exception) {
        super(errorCode, exception);
    }

    public VpnOtpException(String message, Throwable cause) {
        super(message, cause);
    }

    public VpnOtpException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
