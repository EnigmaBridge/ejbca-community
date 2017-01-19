package org.ejbca.core.ejb.vpn;

import org.cesecore.ErrorCode;

/**
 * OTP error - invalid descriptor
 *
 * Created by dusanklinec on 19.01.17.
 */
public class VpnOtpDescriptorException extends VpnOtpException {
    public VpnOtpDescriptorException() {
    }

    public VpnOtpDescriptorException(String message) {
        super(message);
    }

    public VpnOtpDescriptorException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VpnOtpDescriptorException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public VpnOtpDescriptorException(Exception exception) {
        super(exception);
    }

    public VpnOtpDescriptorException(ErrorCode errorCode, Exception exception) {
        super(errorCode, exception);
    }

    public VpnOtpDescriptorException(String message, Throwable cause) {
        super(message, cause);
    }

    public VpnOtpDescriptorException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
