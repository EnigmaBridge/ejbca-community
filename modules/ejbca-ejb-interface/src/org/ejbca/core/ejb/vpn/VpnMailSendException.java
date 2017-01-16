package org.ejbca.core.ejb.vpn;

import org.cesecore.CesecoreException;
import org.cesecore.ErrorCode;

/**
 * Exception on mail send failure.
 *
 * Created by dusanklinec on 16.01.17.
 */
public class VpnMailSendException extends CesecoreException {

    private static final long serialVersionUID = 1L;

    public VpnMailSendException() {
    }

    public VpnMailSendException(String message) {
        super(message);
    }

    public VpnMailSendException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VpnMailSendException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public VpnMailSendException(Exception exception) {
        super(exception);
    }

    public VpnMailSendException(ErrorCode errorCode, Exception exception) {
        super(errorCode, exception);
    }

    public VpnMailSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public VpnMailSendException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
