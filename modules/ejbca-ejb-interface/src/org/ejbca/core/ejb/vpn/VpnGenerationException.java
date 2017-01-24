package org.ejbca.core.ejb.vpn;

import org.cesecore.ErrorCode;

/**
 * General exception thrown by Vpn Generators
 *
 * @author ph4r05
 * Created by dusanklinec on 12.01.17.
 */
public class VpnGenerationException extends VpnException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of <code>VpnUserNameInUseException</code> without detail message.
     */
    public VpnGenerationException() {
        super();
        super.setErrorCode(ErrorCode.VPN_GENERATOR_EXCEPTION);
    }

    /**
     * Constructs an instance of <code>VpnUserNameInUseException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public VpnGenerationException(String msg) {
        super(ErrorCode.VPN_GENERATOR_EXCEPTION, msg);
    }

    /**
     * Constructs an instance of <code>VpnUserNameInUseException</code> with the specified detail message.
     * @param exception the exception that caused this
     */
    public VpnGenerationException(Exception exception) {
        super(ErrorCode.VPN_GENERATOR_EXCEPTION, exception);
    }

    /**
     * Constructs an instance of <code>VpnUserNameInUseException</code> with the specified detail message.
     * @param msg the detail message.
     * @param e the exception that caused this
     */
    public VpnGenerationException(String msg, Exception e) {
        super(ErrorCode.VPN_GENERATOR_EXCEPTION, msg, e);
    }
}
