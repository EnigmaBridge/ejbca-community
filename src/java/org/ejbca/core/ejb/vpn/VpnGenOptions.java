package org.ejbca.core.ejb.vpn;

import org.ejbca.core.ejb.vpn.useragent.OperatingSystem;

import java.io.Serializable;

/**
 * VPN config file generating options
 * Created by dusanklinec on 20.03.17.
 */
public class VpnGenOptions implements Serializable{
    private OperatingSystem os;

    public VpnGenOptions() {
    }

    public VpnGenOptions(OperatingSystem os) {
        this.os = os;
    }

    public OperatingSystem getOs() {
        return os;
    }

    public void setOs(OperatingSystem os) {
        this.os = os;
    }
}
