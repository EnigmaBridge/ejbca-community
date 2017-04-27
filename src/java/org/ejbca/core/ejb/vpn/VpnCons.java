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
    public static final String DEFAULT_CERTIFICATE_PROFILE_SERVER_VPN = "VPNSERVER";
    public static final String DEFAULT_CERTIFICATE_PROFILE_CLIENT = "VPNCLIENT";
    public static final long DEFAULT_VPN_SERVER_VALIDITY = 10*366;
    public static final long DEFAULT_VPN_CLIENT_VALIDITY = 2*366;
    public static final String DEFAULT_KEYSTORE_PASS = "enigma";
    public static final String DEFAULT_KEY_ALGORITHM = AlgorithmConstants.KEYALGORITHM_RSA;
    public static final String DEFAULT_KEY_SIZE = "2048";
    public static final String DEFAULT_TEMPLATE_DIR = "vpn_templates";
    public static final String DEFAULT_LANGUAGE_DIR = "vpn_lang";
    public static final String DEFAULT_LANGUAGE = "en";
    public static final long DEFAULT_VPN_OVERLAP = 60L * 60L * 1000L;
    public static final boolean DEFAULT_VPN_CRL_MOVE = true;
    public static final boolean DEFAULT_VPN_CRL_REFRESH_ON_REVOKE = true;
    public static final boolean DEFAULT_VPN_CRL_REFRESH_FILE_ON_REVOKE = true;
    public static final String DEFAULT_CONFIG_VPN_DOWNLOAD_TITLE = "Enigma Bridge Private Space";

    public static final String DEFAULT_VPN_SUBNET_ADDRESS = "10.8.0.0";
    public static final int DEFAULT_VPN_SUBNET_SIZE = 24;
    public static final String DEFAULT_VPN_VPN_SERVER = "10.8.0.1";

    public static final String VPN_SERVER_USERNAME = "VPN Server";
    public static final String VPN_LANGUAGE_FILE = "languagefile";

    public static final String VPN_EMAIL_TEMPLATE = "emailconfig";
    public static final String VPN_CONFIG_TEMPLATE = "vpnconfig";

    /**
     * VPN OTP property keys
     */
    public static final String KEY_METHOD = "method";
    public static final String KEY_USER_AGENT = "ua";
    public static final String KEY_IP = "ip";
    public static final String KEY_FORWARDED = "fwded";

    public static final String OTP_NONCE = "nonce";
    public static final String OTP_NONCE_CNT = "cnt";
    public static final String OTP_NONCE_TIME = "time";

    public static final String KEY_RETURN_KEY_STORE = "returnKeyStore";

    /**
     * VPN data dir - default one.
     */
    public static final String VPN_DATA = "vpn";

    /**
     * OTP downloading - index bean
     */
    public static final String OTP_SUPERADMIN = "superadmin";
    public static final String OTP_TYPE_P12 = "p12";
    public static final String OTP_AUX_P12_PATH = "p12_path";

    /**
     * VPN config json keys
     */
    public static final String VPN_CFG = "config";
    public static final String VPN_CFG_VERSION = "version";
    public static final String VPN_CFG_HOSTNAME = "vpn_hostname";
    public static final String VPN_CFG_ENTITY_USERNAME = "entity_username";
    public static final String VPN_CFG_USER = "user";
    public static final String VPN_CFG_CA = "vpn_ca";
    public static final String VPN_CFG_CERT = "vpn_cert";
    public static final String VPN_CFG_KEY = "vpn_key";

    /**
     * Admin roles
     */
    public static final String ROLE_SUPERADMIN = "superadmin";
    public static final String ROLE_NONE = "__NONE__";
}
