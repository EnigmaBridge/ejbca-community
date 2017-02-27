package org.ejbca.ui.web.pub.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.util.CertTools;
import org.cesecore.vpn.OtpDownload;
import org.cesecore.vpn.VpnUser;
import org.ejbca.core.ejb.vpn.*;
import org.ejbca.core.ejb.vpn.useragent.OperatingSystem;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;

/**
 * Managed web bean for p12 file download
 *
 * @author ph4r05
 * Created by dusanklinec on 25.01.17.
 */
public class P12Bean extends BaseWebBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(P12Bean.class);
    public static final String LINK_ERROR_SESSION = "otpP12LinkError";
    public static final String DOWNLOADED_COOKIE = "fileDownload";

    private String otp;
    private Boolean otpValid;
    private OtpDownload token;
    private Exception exception;
    private VpnLinkError linkError;
    private Date dateGenerated;
    private String landingLink;
    private String adminLink;
    private String indexLink;
    private String p12FileName;
    private Boolean connectedFromVpn;

    private final CaSessionLocal caSession = ejb.getCaSession();

    /**
     * Initialisation on load
     * @param request HttpServletRequest
     * @throws Exception
     */
    public void initialize(HttpServletRequest request) throws Exception {
        authToken = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("Public Web: "+request.getRemoteAddr()));
        this.request = request;

        userAgentParse(request);
        ip = request.getRemoteAddr();
        adminLink = buildPrivateSpaceAdminPageLink();
        indexLink = buildPrivateSpaceIndexLink();

        loadParams();
        checkOtp();
    }

    /**
     * Cleans state of the bean - after new OTP parameters have been passed
     */
    private void clean(){
        otpValid = null;
        token = null;
        exception = null;
        linkError = VpnLinkError.NONE;
        dateGenerated = null;
        landingLink = null;
        p12FileName = null;
    }

    /**
     * Checks OTP
     */
    public void checkOtp(){
        if (otp == null || exception != null || linkError != VpnLinkError.NONE) {
            otpValid = false;
            return;
        }

        try {
            final Properties properties = buildDescriptorProperties(request, null);
            token = vpnUserManagementSession.otpCheckOtp(authToken, otp, properties);

            otpValid = true;
            exception = null;
            linkError = getConnectedFromVpn() ? VpnLinkError.NONE : VpnLinkError.NOT_IN_VPN;
            dateGenerated = new Date(token.getDateCreated());
            landingLink = buildLandingLink(token.getOtpDownload());
            p12FileName = VpnUtils.getP12FileNameHuman(token);

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
     * Generates OTP landing link
     * @param otp otp code
     * @return absolute link
     */
    private String buildLandingLink(String otp){
        return buildP12Link(otp);
    }

    /**
     * Loads params from the request / session.
     */
    public void loadParams() {
        final String otpString = request.getParameter("otp");
        boolean changed = false;

        if (otpString != null){
            this.otp = !otpString.isEmpty() ? otpString : null;
            changed = true;
        }

        // LinkError from session - download servlet
        try {
            final String errorLinkString = (String)request.getSession().getAttribute(P12Bean.LINK_ERROR_SESSION);
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
        return String.format("getp12?otp=%s", otp);
    }

    /**
     * Returns OVPN download link - landing page.
     * @return link for the download
     */
    public String getLandingLink() {
        return landingLink;
    }

    /**
     * Returns true if user is connected from VPN
     * @return true if user connects using VPN
     */
    public boolean getConnectedFromVpn(){
        try {
            connectedFromVpn = VpnUtils.isIpInVPNNetwork(this.ip);
            return connectedFromVpn;
        } catch (UnknownHostException e){
            log.error("Exception when checking IP in VPN", e);
        }

        return false;
    }

    public String getOtp() {
        return otp;
    }

    public boolean isOtpValid() {
        if (otpValid == null){
            checkOtp();
        }

        return otpValid;
    }

    public OtpDownload getToken() {
        return token;
    }

    public Exception getException() {
        return exception;
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

    public String getP12FileName() {
        return p12FileName;
    }

    public String getAdminLink() {
        return adminLink;
    }

    public String getIndexLink() {
        return indexLink;
    }
}
