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
    public static final String DEFAULT_TEMPLATE_DIR = "vpn_templates";
    public static final String DEFAULT_LANGUAGE_DIR = "vpn_lang";
    public static final String DEFAULT_LANGUAGE = "en";

    public static final String VPN_SERVER_USERNAME = "VPN Server";
    public static final String VPN_LANGUAGE_FILE = "languagefile";

    public static final String VPN_EMAIL_TEMPLATE = "emailconfig";
    public static final String VPN_CONFIG_TEMPLATE = "vpnconfig";

    public static final String KEY_METHOD = "method";
    public static final String KEY_USER_AGENT = "ua";
    public static final String KEY_IP = "ip";
    public static final String KEY_FORWARDED = "fwded";

}
