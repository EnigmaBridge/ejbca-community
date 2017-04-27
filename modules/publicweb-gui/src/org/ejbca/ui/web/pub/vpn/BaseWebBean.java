package org.ejbca.ui.web.pub.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.ejbca.core.ejb.vpn.VpnConfig;
import org.ejbca.core.ejb.vpn.VpnCons;
import org.ejbca.core.ejb.vpn.VpnUserManagementSession;
import org.ejbca.core.ejb.vpn.useragent.OperatingSystem;
import org.ejbca.core.model.util.EjbLocalHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Properties;

/**
 * Base web bean
 * Created by dusanklinec on 22.02.17.
 */
public abstract class BaseWebBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(BaseWebBean.class);

    protected AuthenticationToken authToken;

    protected String browser = "unknown";
    protected OperatingSystem os = OperatingSystem.UNKNOWN;
    protected String hostname;
    protected String ip;

    protected final EjbLocalHelper ejb = new EjbLocalHelper();
    protected final VpnUserManagementSession vpnUserManagementSession = ejb.getVpnUserManagementSession();

    protected HttpServletRequest request;

    /**
     * Returns the detected browser type.
     * @return Either "netscape", "explorer" or "unknown"
     */
    public String getBrowser() {
        return browser;
    }

    /**
     * Private space host name
     * @return hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Returns detected OS
     * @return
     */
    public OperatingSystem getOs() {
        return os;
    }

    /**
     * Returns detected OS group
     * @return
     */
    public OperatingSystem getOsGroup() {
        return os.getGroup();
    }

    /**
     * Returns true if detected OS is not a desktop.
     * @return
     */
    public boolean getIsMobileDevice(){
        final OperatingSystem grp = os.getGroup();
        return grp != OperatingSystem.WINDOWS && grp != OperatingSystem.LINUX && grp != OperatingSystem.MAC_OS_X;
    }

    /**
     * Builds properties descriptor
     * @param request http request
     * @return properties
     */
    public static Properties buildDescriptorProperties(HttpServletRequest request, Properties properties){
        if (properties == null){
            properties = new Properties();
        }

        final String xFwded = request.getHeader("X-Forwarded-For");
        final String ip = request.getRemoteAddr();
        final String ua = request.getHeader("User-Agent");
        final String method = request.getMethod();
        final String qrNonce = request.getParameter("qrnonce");

        properties.setProperty(VpnCons.KEY_IP, ip+"");
        properties.setProperty(VpnCons.KEY_FORWARDED, xFwded+"");
        properties.setProperty(VpnCons.KEY_USER_AGENT, ua+"");
        properties.setProperty(VpnCons.KEY_METHOD, method+"");
        properties.setProperty(VpnCons.KEY_QR_NONCE, qrNonce+"");
        return properties;
    }

    /**
     * User agent parsing.
     * Detects the browser type from the User-Agent HTTP header and returns it.
     */
    protected void userAgentParse(HttpServletRequest request) {
        final String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            os = OperatingSystem.parseUserAgentString(userAgent);

            final boolean isGecko = userAgent.contains("Gecko");
            final boolean isIE = userAgent.contains("MSIE");
            final boolean isNewIE = userAgent.contains("Trident"); // IE11

            if (isIE && !isGecko)
                browser = "explorer";
            if (isNewIE)
                browser = "explorer";
            if (isGecko)
                browser = "netscape";
        } else{
            log.info("User agent is null");
            browser = "unknown";
        }
    }

    /**
     * Generates OTP landing link
     * @param otp otp code
     * @return absolute link
     */
    public String buildP12Link(String otp){
        final int port = VpnConfig.getPublicHttpsPort();
        final String hostname = VpnConfig.getServerHostname();
        return String.format("https://%s:%d/ejbca/vpn/p12.jsf?otp=%s", hostname, port, otp);
    }

    /**
     * Builds link to the private space administration
     * @return absolute link
     */
    public String buildPrivateSpaceAdminPageLink(){
        return buildPrivateSpaceAdminPageLink(true);
    }

    /**
     * Builds link to the private space administration
     * @return absolute link
     */
    public String buildPrivateSpaceAdminPageLink(boolean withClientCert){
        final int port = withClientCert ? VpnConfig.getPrivateHttpsPort() : VpnConfig.getPublicHttpsPort();
        final String hostname = VpnConfig.getServerHostname();
        return String.format("https://%s:%d/admin", hostname, port);
    }

    /**
     * Builds link to the private space index page on the public https port
     * @return absolute link
     */
    public String buildPrivateSpaceIndexLink(){
        final int port = VpnConfig.getPublicHttpsPort();
        final String hostname = VpnConfig.getServerHostname();
        return String.format("https://%s:%d/", hostname, port);
    }


}