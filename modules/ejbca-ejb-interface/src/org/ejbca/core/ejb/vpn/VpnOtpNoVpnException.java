package org.ejbca.core.ejb.vpn;

import org.cesecore.ErrorCode;

/**
 * OTP error - OTP token have to be used in VPN network.
 *
 * Created by dusanklinec on 19.01.17.
 */
public class VpnOtpNoVpnException extends VpnOtpException {
    public VpnOtpNoVpnException() {
    }

    public VpnOtpNoVpnException(String message) {
        super(message);
    }

    public VpnOtpNoVpnException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VpnOtpNoVpnException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public VpnOtpNoVpnException(Exception exception) {
        super(exception);
    }

    public VpnOtpNoVpnException(ErrorCode errorCode, Exception exception) {
        super(errorCode, exception);
    }

    public VpnOtpNoVpnException(String message, Throwable cause) {
        super(message, cause);
    }

    public VpnOtpNoVpnException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
