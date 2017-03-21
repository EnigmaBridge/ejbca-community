package org.ejbca.ui.web.pub.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.certificates.certificate.CertificateStoreSessionLocal;
import org.cesecore.configuration.ConfigurationBase;
import org.cesecore.configuration.GlobalConfigurationData;
import org.cesecore.configuration.GlobalConfigurationSessionLocal;
import org.cesecore.vpn.OtpDownload;
import org.cesecore.vpn.VpnUser;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.core.ejb.authentication.web.WebAuthenticationProviderSessionLocal;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionLocal;
import org.ejbca.core.ejb.vpn.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Backing bean for the index page
 * Created by dusanklinec on 23.02.17.
 */
public class IndexBean  extends BaseWebBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(IndexBean.class);

    private EndEntityManagementSessionLocal endEntityManagementSessionLocal = ejb.getEndEntityManagementSession();
    private WebAuthenticationProviderSessionLocal authenticationSessionLocal = ejb.getWebAuthenticationProviderSession();
    private AccessControlSessionLocal accessControlSessionLocal = ejb.getAccessControlSession();
    private GlobalConfigurationSessionLocal configurationSessionLocal = ejb.getGlobalConfigurationSession();
    private VpnUserManagementSessionLocal vpnUserManagementSession = ejb.getVpnUserManagementSession();
    private CertificateStoreSessionLocal certificateStoreSessionLocal = ejb.getCertificateStoreSession();

    private VpnWebUtils.AdminAuthorization adminChecker;
    private Boolean connectedFromVpn;
    private Boolean isAdmin;
    private Boolean isAdminP12Available;
    private Boolean isOnlyAdmin;
    private Boolean isVpnDownloaded;
    private Boolean isP12downloadFlowEnabled;
    private OtpDownload otpDownload;
    private VpnUser vpnUser;
    private GlobalConfiguration globalConfiguration;
    private String hostPort;
    private String spaceName;
    private String adminPageLink;

    /**
     * Initialisation on load
     * @param request HttpServletRequest
     */
    public void initialize(HttpServletRequest request, HttpServletResponse response) throws Exception {
        authToken = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("Public Web: "+request.getRemoteAddr()));
        this.request = request;
        this.ip = request.getRemoteAddr();
        this.adminChecker = VpnWebUtils.buildAdminChecker(authenticationSessionLocal,
                endEntityManagementSessionLocal,
                accessControlSessionLocal,
                vpnUserManagementSession,
                certificateStoreSessionLocal);

        this.isAdmin = null;
        this.isAdminP12Available = null;
        this.connectedFromVpn = null;
        this.otpDownload = null;
        this.vpnUser = null;
        this.isOnlyAdmin = null;
        this.isVpnDownloaded = null;
        this.globalConfiguration = null;
        this.adminPageLink = buildPrivateSpaceAdminPageLink();
        this.hostname = VpnConfig.getServerHostname();
        this.hostPort = VpnWebUtils.getRequestServerName(request);
        this.spaceName = VpnUtils.getHostnameId();
        this.isP12downloadFlowEnabled = VpnConfig.isP12DownloadFlowEnabled();

        userAgentParse(request);
        loadVpnAdminUser();
        checkVpnDownloaded();
        getIsAdminP12Available();
        getIsAdmin();
        getConnectedFromVpn();

        if (connectedFromVpn){
            log.info("Connected via VPN -> VPN was downloaded");
            isVpnDownloaded = true;
        }

        // Admin -> redirect to the admin page
        if (isAdmin){
            log.info("Redirecting to admin");
            response.sendRedirect(buildPrivateSpaceAdminPageLink(isP12downloadFlowEnabled));
            response.flushBuffer();
            return;
        }

        // Using VPN & p12 for download -> redirect to p12 download page
        if (isOnlyAdmin && connectedFromVpn && isAdminP12Available && isP12downloadFlowEnabled){
            log.info("Redirecting to p12 download");
            response.sendRedirect(buildP12Link(otpDownload.getOtpDownload()));
            response.flushBuffer();
            return;
        }

        try {
            globalConfiguration = (GlobalConfiguration) configurationSessionLocal
                    .getCachedConfiguration(GlobalConfiguration.GLOBAL_CONFIGURATION_ID);
            if (globalConfiguration.getAdminLoggedInSuccessfully()){
                log.info("Admin logged in successfully, redirecting to admin");
                response.sendRedirect(buildPrivateSpaceAdminPageLink());
                response.flushBuffer();
                return;
            }
        } catch(Exception e){
            log.error("Could not load global configuration");
        }
    }

    /**
     * Loads the only one admin user - it is an admin
     * @return vpn admin user
     */
    public VpnUser loadVpnAdminUser(){
        if (vpnUser == null){
            // Allow only one user
            final List<Integer> userIds = vpnUserManagementSession.geVpnUsersIds(authToken);
            if (userIds == null || userIds.size() != 1){
                log.info("VPN User IDs list does not have the correct size: " + (userIds == null ? -1 : userIds.size()));
                isOnlyAdmin = false;
                return null;
            }

            isOnlyAdmin = true;
            try {
                vpnUser = vpnUserManagementSession.getVpnUser(authToken, userIds.get(0));
            }catch(AuthorizationDeniedException e){
                log.error("Could not load VPN user", e);
            }
        }

        return vpnUser;
    }

    /**
     * Loads info if VPN profile has been downloaded
     */
    public boolean checkVpnDownloaded(){
        if (vpnUser == null){
            log.info("VPN user is null, vpn downloaded = false");
            isVpnDownloaded = false;
            return isVpnDownloaded;
        }

        isVpnDownloaded = vpnUser.getConfigVersion() > 2 || vpnUser.getOtpUsedCount() > 0;
        return isVpnDownloaded;
    }

    /**
     * Checks if admin P12 OTP is available for download.
     * Does data load to the internal state.
     * @return true if admin p12 OTP is ready
     */
    public boolean getIsAdminP12Available(){
        if (isAdminP12Available == null){
            try {
                otpDownload = vpnUserManagementSession.otpGet(authToken, VpnCons.OTP_TYPE_P12, VpnCons.OTP_SUPERADMIN, null);
                if (otpDownload == null){
                    isAdminP12Available = false;
                    return false;
                }

                if (otpDownload.getOtpFirstUsed() != null && otpDownload.getOtpFirstUsed() > 0){
                    isAdminP12Available = false;
                    return false;
                }

                if (otpDownload.getOtpUsedCount() > 0){
                    isAdminP12Available = false;
                    return false;
                }

                isAdminP12Available = true;

            } catch (AuthorizationDeniedException e) {
                log.error("Auth denied - unable to fetch the token");
            }
        }

        return isAdminP12Available;
    }

    /**
     * Returns true if the user is admin - has client certificate and admin rights.
     * This means he can be redirected to the admin interface.
     * @return
     */
    public boolean getIsAdmin(){
        if (isAdmin == null){
            isAdmin = adminChecker.tryIsAdmin(request);
        }

        return isAdmin;
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

        connectedFromVpn = false;
        return false;
    }

    public Boolean getAdmin() {
        return isAdmin;
    }

    public Boolean getAdminP12Available() {
        return isAdminP12Available;
    }

    public Boolean getOnlyAdmin() {
        return isOnlyAdmin;
    }

    public Boolean getVpnDownloaded() {
        return isVpnDownloaded;
    }

    public String getAdminPageLink() {
        return adminPageLink;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public Boolean getP12downloadFlowEnabled() {
        return isP12downloadFlowEnabled;
    }
}
