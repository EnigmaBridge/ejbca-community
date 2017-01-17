package org.ejbca.core.ejb.vpn;

import org.cesecore.CesecoreException;
import org.cesecore.ErrorCode;

/**
 * General VPN exception.
 *
 * @author ph4r05
 * Created by dusanklinec on 17.01.17.
 */
public class VpnException extends CesecoreException {
    public VpnException() {
    }

    public VpnException(String message) {
        super(message);
    }

    public VpnException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VpnException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public VpnException(Exception exception) {
        super(exception);
    }

    public VpnException(ErrorCode errorCode, Exception exception) {
        super(errorCode, exception);
    }

    public VpnException(String message, Throwable cause) {
        super(message, cause);
    }

    public VpnException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

}
