package org.ejbca.core.ejb.vpn;

import org.ejbca.config.EjbcaConfigurationHolder;

/**
 * VPN related configuration.
 *
 * Created by dusanklinec on 12.01.17.
 */
public class VpnConfig {
    public static final String CONFIG_VPN_CA = "vpn.ca";
    public static final String CONFIG_VPN_CLIENT_END_PROFILE = "vpn.client.endprofile";
    public static final String CONFIG_VPN_SERVER_END_PROFILE = "vpn.server.endprofile";
    public static final String CONFIG_VPN_KEYSTORE_PASS = "vpn.keystorepass";

    private static String getDefaultIfEmpty(String src, String defaultValue){
        return (src == null || src.isEmpty()) ? defaultValue : src;
    }

    /**
     * The configured VPN CA name.
     * @return CA name
     */
    public static String getCA() {
        return getDefaultIfEmpty(EjbcaConfigurationHolder.getExpandedString(CONFIG_VPN_CA),
                VpnCons.DEFAULT_CA);
    }

    /**
     * The configured end entity profile name for VPN clients.
     * @return client end entity profile name
     */
    public static String getClientEndEntityProfile() {
        return getDefaultIfEmpty(EjbcaConfigurationHolder.getExpandedString(CONFIG_VPN_CLIENT_END_PROFILE),
                VpnCons.DEFAULT_END_ENTITY_PROFILE);
    }

    /**
     * The configured end entity profile name for VPN server.
     * @return server end entity profile name
     */
    public static String getServerEndEntityProfile() {
        return getDefaultIfEmpty(EjbcaConfigurationHolder.getExpandedString(CONFIG_VPN_SERVER_END_PROFILE),
                VpnCons.DEFAULT_END_ENTITY_PROFILE_SERVER);
    }

    /**
     * The configured VpnUser KeyStore password.
     * @return key store password
     */
    public static String getKeyStorePass(){
        return getDefaultIfEmpty(EjbcaConfigurationHolder.getExpandedString(CONFIG_VPN_KEYSTORE_PASS),
                VpnCons.DEFAULT_KEYSTORE_PASS);
    }


}
