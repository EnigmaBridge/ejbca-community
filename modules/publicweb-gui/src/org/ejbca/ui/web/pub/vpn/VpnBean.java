package org.ejbca.ui.web.pub.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.util.CertTools;
import org.cesecore.vpn.VpnUser;
import org.ejbca.core.ejb.vpn.*;
import org.ejbca.core.ejb.vpn.useragent.OperatingSystem;
import org.ejbca.core.model.util.EjbLocalHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Date;
import java.util.Properties;

/**
 * VPN managed bean for VPN config download
 *
 * @author ph4r05
 * Created by dusanklinec on 25.01.17.
 */
public class VpnBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(VpnBean.class);
    public static final String LINK_ERROR_SESSION = "otpLinkError";
    public static final String DOWNLOADED_COOKIE = "fileDownload";

    private AuthenticationToken authToken;

    private String browser = "unknown";
    private OperatingSystem os = OperatingSystem.UNKNOWN;

    private String otp;
    private Integer vpnUserId;
    private Boolean otpValid;
    private VpnUser vpnUser;
    private Exception exception;
    private VpnLinkError linkError;
    private Date dateGenerated;
    private String landingLink;
    private String hostname;

    private HttpServletRequest request;
    private final EjbLocalHelper ejb = new EjbLocalHelper();
    private final VpnUserManagementSession vpnUserManagementSession = ejb.getVpnUserManagementSession();
    private final CaSessionLocal caSession = ejb.getCaSession();

    /**
     * Initialisation on load
     * @param request HttpServletRequest
     * @throws Exception
     */
    public void initialize(HttpServletRequest request) throws Exception {
        authToken = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("Public Web: "+request.getRemoteAddr()));
        this.request = request;

        // Checking OTP token.
        userAgentParse(request);
        loadParams();
        checkOtp();
    }

    /**
     * Cleans state of the bean - after new OTP parameters have been passed
     */
    private void clean(){
        otpValid = null;
        vpnUser = null;
        exception = null;
        linkError = VpnLinkError.NONE;
        dateGenerated = null;
        landingLink = null;
    }

    /**
     * Checks OTP
     */
    public void checkOtp(){
        if (vpnUserId == null || otp == null || exception != null || linkError != VpnLinkError.NONE) {
            otpValid = false;
            return;
        }

        try {
            final Properties properties = VpnBean.buildDescriptorProperties(request, null);
            vpnUser = vpnUserManagementSession.checkOtp(authToken, vpnUserId, otp, properties);

            otpValid = true;
            exception = null;
            linkError = VpnLinkError.NONE;
            dateGenerated = new Date(vpnUser.getConfigGenerated());
            landingLink = vpnUserManagementSession.getConfigDownloadLink(authToken, vpnUser.getId());

            final CAInfo vpnCA = caSession.getCAInfo(authToken, VpnConfig.getCA());
            hostname = CertTools.getPartFromDN(vpnCA.getSubjectDN(), "CN");

        } catch (VpnOtpInvalidException e) {
            exception = e;
            linkError = VpnLinkError.OTP_INVALID;
            log.info("Exception - otp invalid", e);

        } catch (VpnOtpTooManyException e) {
            exception = e;
            linkError = VpnLinkError.OTP_TOO_MANY;
            log.info("Exception - too many", e);

        } catch (VpnOtpOldException e) {
            exception = e;
            linkError = VpnLinkError.OTP_OLD;
            log.info("Exception - too old", e);

        } catch (VpnNoConfigException e) {
            exception = e;
            linkError = VpnLinkError.NO_CONFIGURATION;
            log.info("Exception - no config", e);

        } catch (VpnOtpDescriptorException e) {
            exception = e;
            linkError = VpnLinkError.OTP_DESCRIPTOR;
            log.info("Exception - descriptor", e);

        } catch(Exception e){
            exception = e;
            linkError = VpnLinkError.GENERIC;
            log.info("Exception - generic", e);
        }
    }

    /**
     * Loads params from the request / session.
     */
    public void loadParams() {
//        final FacesContext cInst = FacesContext.getCurrentInstance();
//        final ExternalContext ctx = cInst.getExternalContext();
//        final String vpnUserIdString = ctx.getRequestParameterMap().get("id");
//        final String otpString = ctx.getRequestParameterMap().get("otp");

        final String vpnUserIdString = request.getParameter("id");
        final String otpString = request.getParameter("otp");
        boolean changed = false;

        if (vpnUserIdString != null) {
            if (vpnUserIdString.isEmpty()){
                this.vpnUserId = null;
                changed = true;

            } else {
                try {
                    int vpnUserId = Integer.parseInt(vpnUserIdString);
                    // If there is a query parameter present and the id is different we flush the cache!
                    if (this.vpnUserId == null || vpnUserId != this.vpnUserId) {
                        this.vpnUserId = vpnUserId;
                        changed = true;
                    }

                } catch (NumberFormatException e) {
                    log.info("Bad 'id' parameter value set, but not a number.");
                }
            }
        }

        if (otpString != null){
            this.otp = !otpString.isEmpty() ? otpString : null;
            changed = true;
        }

        // LinkError from session - download servlet
        try {
            final String errorLinkString = (String)request.getSession().getAttribute(VpnBean.LINK_ERROR_SESSION);
            if (errorLinkString != null && !errorLinkString.isEmpty()) {
                linkError = VpnLinkError.valueOf(errorLinkString);
                log.info("Link error loaded from session: " + linkError);
            }

        } catch(NullPointerException e){
        } catch(Exception e){
            log.error("Error in parsing link error from the session ", e);
        }

        if (changed){
            clean();
        }
    }

    /**
     * User agent parsing.
     * Detects the browser type from the User-Agent HTTP header and returns it.
     */
    private void userAgentParse(HttpServletRequest request) {
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
     * Returns the detected browser type.
     * @return Either "netscape", "explorer" or "unknown"
     */
    public String getBrowser() {
        return browser;
    }

    /**
     * Builds OVPN download link.
     * @return link for the download
     */
    public String getDownloadLink(){
        return String.format("getvpn?id=%s&otp=%s", vpnUserId, otp);
    }

    /**
     * Returns OVPN download link - landing page.
     * @return link for the download
     */
    public String getLandingLink() {
        return landingLink;
    }

    /**
     * Private space host name
     * @return hostname
     */
    public String getHostname() {
        return hostname;
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

        properties.setProperty(VpnCons.KEY_IP, ip+"");
        properties.setProperty(VpnCons.KEY_FORWARDED, xFwded+"");
        properties.setProperty(VpnCons.KEY_USER_AGENT, ua+"");
        properties.setProperty(VpnCons.KEY_METHOD, method+"");
        return properties;
    }

    public String getOtp() {
        return otp;
    }

    public Integer getVpnUserId() {
        return vpnUserId;
    }

    public boolean isOtpValid() {
        if (otpValid == null){
            checkOtp();
        }

        return otpValid;
    }

    public VpnUser getVpnUser() {
        return vpnUser;
    }

    public Exception getException() {
        return exception;
    }

    public OperatingSystem getOs() {
        return os;
    }

    public OperatingSystem getOsGroup() {
        return os.getGroup();
    }

    public Boolean getOtpValid() {
        return otpValid;
    }

    public VpnLinkError getLinkError() {
        return linkError;
    }

    public Date getDateGenerated() {
        return dateGenerated;
    }
}
