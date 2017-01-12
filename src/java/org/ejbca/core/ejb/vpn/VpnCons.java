package org.ejbca.core.ejb.vpn;

import org.cesecore.certificates.util.AlgorithmConstants;

/**
 * VPN related constants.
 *
 * @author ph4r05
 * Created by dusanklinec on 10.01.17.
 */
public class VpnCons {
    public static final String DEFAULT_CA = "VPN";
    public static final String DEFAULT_END_ENTITY_PROFILE = "VPN";
    public static final String DEFAULT_END_ENTITY_PROFILE_SERVER = "VPNServer";
    public static final String DEFAULT_KEYSTORE_PASS = "enigma";
    public static final String DEFAULT_KEY_ALGORITHM = AlgorithmConstants.KEYALGORITHM_RSA;
    public static final String DEFAULT_KEY_SIZE = "2048";

}
