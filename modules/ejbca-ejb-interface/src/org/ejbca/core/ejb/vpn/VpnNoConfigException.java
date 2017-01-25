package org.ejbca.core.ejb.vpn;

import org.cesecore.ErrorCode;

/**
 * VPN - no configuration present exception.
 *
 * Created by dusanklinec on 19.01.17.
 */
public class VpnNoConfigException extends VpnException {
    public VpnNoConfigException() {
    }

    public VpnNoConfigException(String message) {
        super(message);
    }

    public VpnNoConfigException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VpnNoConfigException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public VpnNoConfigException(Exception exception) {
        super(exception);
    }

    public VpnNoConfigException(ErrorCode errorCode, Exception exception) {
        super(errorCode, exception);
    }

    public VpnNoConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public VpnNoConfigException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
