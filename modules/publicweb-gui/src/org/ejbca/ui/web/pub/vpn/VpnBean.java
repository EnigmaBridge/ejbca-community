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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * VPN managed bean for VPN config download
 *
 * @author ph4r05
 * Created by dusanklinec on 25.01.17.
 */
public class VpnBean extends BaseWebBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(VpnBean.class);
    public static final String LINK_ERROR_SESSION = "otpLinkError";
    public static final String DOWNLOADED_COOKIE = "fileDownload";

    public static final String LAST_OTP_TOKEN_DOWNLOADED = "pspaceLastOtpTokenDownloaded";

    private String otp;
    private Integer vpnUserId;
    private Boolean otpValid;
    private Boolean clientInstalledCheck = false;
    private VpnUser vpnUser;
    private Exception exception;
    private VpnLinkError linkError;
    private Date dateGenerated;
    private String landingLink;

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
            final Properties properties = buildDescriptorProperties(request, null);
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

    public Boolean getOtpValid() {
        return otpValid;
    }

    /**
     * Returns true if the current OTP token was already downloaded recently
     * i.e., it is the last OTP token downloaded in the current session.
     * @return true if downloaded
     */
    public Boolean getOtpAlreadyDownloadedSession(){
        if (otp == null){
            return false;
        }

        try {
            final String lastDownloaded = (String) request.getSession().getAttribute(VpnBean.LAST_OTP_TOKEN_DOWNLOADED);
            return lastDownloaded != null && !lastDownloaded.isEmpty() && otp.equals(lastDownloaded);

        } catch(Exception e){
            log.error("Error in parsing link error from the session ", e);
        }

        return false;
    }

    /**
     * Returns true if the current OTP token was already downloaded recently
     * i.e., it is the last OTP token downloaded in the current cookie.
     * @return true if downloaded
     */
    public Boolean getOtpAlreadyDownloadedCookie(){
        if (otp == null){
            return false;
        }

        try {
            final List<Cookie> cookies = VpnWebUtils.getCookies(request, VpnBean.LAST_OTP_TOKEN_DOWNLOADED);
            if (cookies.isEmpty()){
                return false;
            }

            return otp.equals(cookies.get(0).getValue());

        } catch(Exception e){
            log.error("Error in parsing link error from the session ", e);
        }

        return false;
    }

    public Boolean getOtpAlreadyDownloaded() {
        if (getOtpAlreadyDownloadedSession()){
            return true;
        }

        return getOtpAlreadyDownloadedCookie();
    }

    public VpnLinkError getLinkError() {
        return linkError;
    }

    public Date getDateGenerated() {
        return dateGenerated;
    }

    public Boolean getClientInstalledCheck() {
        return clientInstalledCheck;
    }

    public void setClientInstalledCheck(Boolean clientInstalledCheck) {
        this.clientInstalledCheck = clientInstalledCheck;
    }
}
