/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ui.web.admin.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.authorization.control.CryptoTokenRules;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.certificate.IllegalKeyException;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileSession;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.keybind.InternalKeyBindingMgmtSessionLocal;
import org.cesecore.util.StringTools;
import org.ejbca.core.ejb.vpn.*;
import org.cesecore.vpn.VpnUser;
import org.ejbca.core.ejb.ca.auth.EndEntityAuthenticationSession;
import org.ejbca.core.ejb.ca.sign.SignSession;
import org.ejbca.core.ejb.ra.*;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionLocal;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.ra.AlreadyRevokedException;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.ui.web.admin.BaseManagedBean;
import org.ejbca.ui.web.admin.rainterface.RAInterfaceBean;
import org.ejbca.ui.web.admin.rainterface.UserView;

import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;
import java.io.IOException;
import java.io.Serializable;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

/**
 * JavaServer Faces Managed Bean for managing VPN users.
 * Session scoped and will cache the list of tokens and keys.
 * 
 * @author ph4r05
 */
public class VpnUsersMBean extends BaseManagedBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(VpnUsersMBean.class);

    /** GUI table representation of a VPN user that can be interacted with. */
    public class VpnUserGuiInfo {
        private Integer id;

        private String userDesc;
        private String email;
        private String device;

        private Date dateCreated;
        private Date dateModified;
        private Date otpUsed;
        private Date lastMailSent;
        private String otpDownload;
        private boolean revoked;
        private Certificate certificate;
        private PrivateKey key;
        private boolean selected = false;

        // End entity view
        private UserView userview;
        private String statusText;

        public VpnUserGuiInfo() {
        }

        public final void regenerateId(){
            userDesc = email + "/" + device; // TODO: Unify the generation
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
            regenerateId();
        }

        public String getUserDesc() {
            return userDesc;
        }

        public String getDevice() {
            return device;
        }

        public void setDevice(String device) {
            this.device = device;
            regenerateId();
        }

        public Date getDateCreated() {
            return dateCreated;
        }

        public void setDateCreated(Date dateCreated) {
            this.dateCreated = dateCreated;
        }

        public Date getDateModified() {
            return dateModified;
        }

        public void setDateModified(Date dateModified) {
            this.dateModified = dateModified;
        }

        public boolean isRevoked() {
            return revoked;
        }

        public void setRevoked(boolean revoked) {
            this.revoked = revoked;
        }

        public Certificate getCertificate() {
            return certificate;
        }

        public void setCertificate(Certificate certificate) {
            this.certificate = certificate;
        }

        public PrivateKey getKey() {
            return key;
        }

        public void setKey(PrivateKey key) {
            this.key = key;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public UserView getUserview() {
            return userview;
        }

        public void setUserview(UserView userview) {
            this.userview = userview;
        }

        public String getStatusText() {
            return statusText;
        }

        public void setStatusText(String statusText) {
            this.statusText = statusText;
        }

        public Date getOtpUsed() {
            return otpUsed;
        }

        public void setOtpUsed(Date otpUsed) {
            this.otpUsed = otpUsed;
        }

        public String getOtpDownload() {
            return otpDownload;
        }

        public void setOtpDownload(String otpDownload) {
            this.otpDownload = otpDownload;
        }

        public Date getLastMailSent() {
            return lastMailSent;
        }

        public void setLastMailSent(Date lastMailSent) {
            this.lastMailSent = lastMailSent;
        }
    }

    /** GUI edit/view representation of a VpnUser that can be interacted with. */
    public class CurrentVpnUserGuiInfo {
        private Integer id;
        private String name = "";
        private String email = "";
        private String device = "default";

        private long dateCreated;
        private long dateModified;
        private Date dateCreatedDate;
        private Date dateModifiedDate;
        private Date dateOtpDownloaded;
        private Date dateMailSent;
        private int revokedStatus;
        private String otpDownload;
        private String certificateId;
        private String certificate;
        private String key;
        private String config;

        // Not stored in db, just indicator
        private boolean sendConfigEmail = true;

        // End entity view
        private UserView userview;

        private CurrentVpnUserGuiInfo() {}

        public CurrentVpnUserGuiInfo(String email, String device) {
            this.email = email;
            this.device = device;
            this.regenerateId();
        }

        public final void regenerateId(){
            name = email + "/" + device; // TODO: Unify the generation
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
            regenerateId();
        }

        public String getDevice() {
            return device;
        }

        public void setDevice(String device) {
            this.device = device;
            regenerateId();
        }

        public long getDateCreated() {
            return dateCreated;
        }

        public void setDateCreated(long dateCreated) {
            this.dateCreated = dateCreated;
        }

        public long getDateModified() {
            return dateModified;
        }

        public void setDateModified(long dateModified) {
            this.dateModified = dateModified;
        }

        public Date getDateCreatedDate() {
            return dateCreatedDate;
        }

        public void setDateCreatedDate(Date dateCreatedDate) {
            this.dateCreatedDate = dateCreatedDate;
        }

        public Date getDateModifiedDate() {
            return dateModifiedDate;
        }

        public void setDateModifiedDate(Date dateModifiedDate) {
            this.dateModifiedDate = dateModifiedDate;
        }

        public int getRevokedStatus() {
            return revokedStatus;
        }

        public void setRevokedStatus(int revokedStatus) {
            this.revokedStatus = revokedStatus;
        }

        public String getOtpDownload() {
            return otpDownload;
        }

        public void setOtpDownload(String otpDownload) {
            this.otpDownload = otpDownload;
        }

        public String getCertificateId() {
            return certificateId;
        }

        public void setCertificateId(String certificateId) {
            this.certificateId = certificateId;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }

        public UserView getUserview() {
            return userview;
        }

        public void setUserview(UserView userview) {
            this.userview = userview;
        }

        public Date getDateOtpDownloaded() {
            return dateOtpDownloaded;
        }

        public void setDateOtpDownloaded(Date dateOtpDownloaded) {
            this.dateOtpDownloaded = dateOtpDownloaded;
        }

        public Date getDateMailSent() {
            return dateMailSent;
        }

        public void setDateMailSent(Date dateMailSent) {
            this.dateMailSent = dateMailSent;
        }

        public boolean isSendConfigEmail() {
            return sendConfigEmail;
        }

        public void setSendConfigEmail(boolean sendConfigEmail) {
            this.sendConfigEmail = sendConfigEmail;
        }
    }

    private List<VpnUserGuiInfo> vpnUserGuiInfos = new ArrayList<VpnUserGuiInfo>();

    @SuppressWarnings("rawtypes") //JDK6 does not support typing for ListDataModel
    private ListDataModel vpnUserGuiList = null;

    private Integer currentVpnUserId = null;
    private CurrentVpnUserGuiInfo currentVpnUser = null;
    private boolean currentVpnUserEditMode = true;  // currentVpnUserId==0 from start

    private final VpnUserManagementSession vpnUserManagementSession = getEjbcaWebBean().getEjb().getVpnUserManagementSession();
    private final EndEntityProfileSessionLocal endEntityProfileSession = getEjbcaWebBean().getEjb().getEndEntityProfileSession();
    private final CertificateProfileSession certificateProfileSession = getEjbcaWebBean().getEjb().getCertificateProfileSession();
    private final EndEntityManagementSession endEntityManagementSession = getEjbcaWebBean().getEjb().getEndEntityManagementSession();
    private final EndEntityAccessSession endEntityAccessSession = getEjbcaWebBean().getEjb().getEndEntityAccessSession();
    private final EndEntityAuthenticationSession endEntityAuthenticationSession = getEjbcaWebBean().getEjb().getEndEntityAuthenticationSession();

    private final AccessControlSessionLocal accessControlSession = getEjbcaWebBean().getEjb().getAccessControlSession();
    private final AuthenticationToken authenticationToken = getAdmin();
    private final CaSessionLocal caSession = getEjbcaWebBean().getEjb().getCaSession();

    /**
     * Registration Authority bean
     */
    private RAInterfaceBean raif;

    /** Workaround to cache the items used to render the page long enough for actions to be able to use them, but reload on every page view. */
    public boolean isPageLoadResetTrigger() {
        flushCaches();
        return false;
    }

    /** Force reload from underlying (cache) layer */
    private void flushCaches() {
        vpnUserGuiList = null;
        flushCurrent();
    }
    
    /** Force reload from underlying (cache) layer for the current CryptoToken and its list of key pairs */
    private void flushCurrent() {
        currentVpnUser = null;
    }

    /**
     * Gets RA bean.
     * @return
     */
    private RAInterfaceBean getRaif() throws IOException, ClassNotFoundException {
        if (raif == null) {
           raif = VpnWebUtils.getRaBean();
        }
        return raif;
    }

    private Date dateOrNull(Long date){
        if (date == null){
            return null;
        }

        return new Date(date);
    }

    public String getStatusText(int status){
        switch(status) {
            case EndEntityConstants.STATUS_NEW:
                return (getEjbcaWebBean().getText("STATUSNEW"));
            case EndEntityConstants.STATUS_FAILED:
                return (getEjbcaWebBean().getText("STATUSFAILED"));
            case EndEntityConstants.STATUS_INITIALIZED:
                return (getEjbcaWebBean().getText("STATUSINITIALIZED"));
            case EndEntityConstants.STATUS_INPROCESS:
                return (getEjbcaWebBean().getText("STATUSINPROCESS"));
            case EndEntityConstants.STATUS_GENERATED:
                return (getEjbcaWebBean().getText("STATUSGENERATED"));
            case EndEntityConstants.STATUS_REVOKED:
                return (getEjbcaWebBean().getText("STATUSREVOKED"));
            case EndEntityConstants.STATUS_HISTORICAL:
                return (getEjbcaWebBean().getText("STATUSHISTORICAL"));
            case EndEntityConstants.STATUS_KEYRECOVERY:
                return (getEjbcaWebBean().getText("STATUSKEYRECOVERY"));
            default:
                return null;
        }
    }

    public String getEndEntityId(VpnUser usr){
        return vpnUserManagementSession.getUserName(usr);
    }

    /** Build a list sorted by name from the authorized VpnUsers that can be presented to the user */
    @SuppressWarnings({ "rawtypes", "unchecked" }) //JDK6 does not support typing for ListDataModel
    public ListDataModel getVpnUserGuiList() throws AuthorizationDeniedException {
        if (vpnUserGuiList ==null) {
            final List<Integer> vpnUserIds = vpnUserManagementSession.geVpnUsersIds(authenticationToken);
            final List<VpnUserGuiInfo> users = new ArrayList<>(vpnUserIds.size());
            final HashMap<Integer, String> caIdToNameMap = caSession.getCAIdToNameMap();

            for(Integer userId : vpnUserIds){
                final VpnUser vpnUser = vpnUserManagementSession.getVpnUser(authenticationToken, userId);
                final VpnUserGuiInfo guiUser = toGuiUser(vpnUser);
                final String endEntityId = getEndEntityId(vpnUser);

                // Load corresponding end entity
                EndEntityInformation endEntity = endEntityAccessSession.findUser(authenticationToken, endEntityId);
                if (endEntity != null) {
                    UserView userview = new UserView(endEntity, caIdToNameMap);
                    guiUser.setUserview(userview);
                    guiUser.setStatusText(getStatusText(userview.getStatus()));

                } else {
                    guiUser.setStatusText(getEjbcaWebBean().getText("VPNINVALIDNOENTITY"));
                }

                users.add(guiUser);
            }

            Collections.sort(users, new Comparator<VpnUserGuiInfo>() {
                @Override
                public int compare(VpnUserGuiInfo a, VpnUserGuiInfo b) {
                    final int cmpEmail = a.getEmail().compareTo(b.getEmail());
                    if (cmpEmail != 0){
                        return cmpEmail;
                    }

                    return a.getDevice().compareTo(b.getDevice());
                }
            });

            vpnUserGuiInfos = users;
            vpnUserGuiList = new ListDataModel(vpnUserGuiInfos);
        }
        // If show the list, then we are on the main page and want to flush the two caches
        flushCurrent();
        setCurrentVpnUserEditMode(false);
        return vpnUserGuiList;
    }

    /** Flushes caches, reloads the page */
    public void refreshPage() {
        flushCaches();
    }

    /** Invoked when admin requests a new VpnUser credentials, revoking the old ones. */
    public void regenerateVpnUsers() throws AuthorizationDeniedException {
        if (vpnUserGuiList == null) {
            return;
        }

        String msg = null;
        try {
            for (VpnUserGuiInfo vpnUserGuiInfo : vpnUserGuiInfos) {
                if (!vpnUserGuiInfo.isSelected()) {
                    continue;
                }

                // Revocation first - if there is some certificate already.
                endEntityManagementSession.revokeUser(authenticationToken, vpnUserGuiInfo.getUserDesc(), 4);

                // Delete VPN related crypto info
                vpnUserManagementSession.revokeVpnUser(authenticationToken, vpnUserGuiInfo.getId());

                // Load fresh VPN user view
                final VpnUser vpnUser = vpnUserManagementSession.getVpnUser(authenticationToken, vpnUserGuiInfo.getId());
                final String endEntityId = getEndEntityId(vpnUser);

                endEntityManagementSession.setUserStatus(authenticationToken, vpnUserGuiInfo.getUserDesc(),
                        EndEntityConstants.STATUS_NEW);

                final EndEntityInformation endEntity = endEntityAccessSession.findUser(authenticationToken, endEntityId);

                // Regenerate certificate
                try {
                    // The password is taken from end entity.
                    // In the current setting we use auto-generated passwords in cleartext.
                    // The new password is generated after a new certificate is generated.
                    generateKeyAndConfig(vpnUser.getId(), Optional.<String>empty());

                } catch (Exception e) {
                    // If things went wrong set status to FAILED
                    final String newStatusString;
                    endEntityManagementSession.setUserStatus(authenticationToken, endEntity.getUsername(),
                            EndEntityConstants.STATUS_FAILED);
                    newStatusString = "FAILED";

                    log.error(InternalEjbcaResources.getInstance().getLocalizedMessage(
                            "vpn.errorsetstatus", newStatusString), e);
                    final String errMsg = InternalEjbcaResources.getInstance().getLocalizedMessage(
                            "vpn.erroruser", endEntity.getUsername());
                    throw new RuntimeException(errMsg, e);
                }

                // Send email.
                vpnUserManagementSession.sendConfigurationEmail(authenticationToken, vpnUserGuiInfo.getId(), null);

                currentVpnUserId = vpnUser.getId();
                msg = "VpnUser regenerated successfully.";
            }

        } catch (ApprovalException | WaitingForApprovalException | FinderException | AlreadyRevokedException e) {
            msg = e.getMessage();
        } catch (IOException | VpnMailSendException e) {
            msg = e.getMessage();
        }

        flushCaches();
        if (msg != null) {
            log.info("Message displayed to user: " + msg);
            super.addNonTranslatedErrorMessage(msg);
        }
    }

    /** Invoked when admin requests a VpnUser certificate revocation. */
    public void revokeVpnUsers() throws AuthorizationDeniedException {
        if (vpnUserGuiList == null) {
            return;
        }

        String msg = null;
        try {
            for (VpnUserGuiInfo vpnUserGuiInfo : vpnUserGuiInfos) {
                if (!vpnUserGuiInfo.isSelected()) {
                    continue;
                }

                // Revocation first. TODO: revocation reason parametrisation
                endEntityManagementSession.revokeUser(authenticationToken, vpnUserGuiInfo.getUserDesc(), 0);

                // Delete VPN related crypto info
                vpnUserManagementSession.revokeVpnUser(authenticationToken, vpnUserGuiInfo.getId());
            }
        } catch (ApprovalException | WaitingForApprovalException | FinderException | AlreadyRevokedException e) {
            msg = e.getMessage();
        }

        flushCaches();
        if (msg != null) {
            log.info("Message displayed to user: " + msg);
            super.addNonTranslatedErrorMessage(msg);
        }
    }

    /** Invoked when admin requests a VpnUsers deletion. */
    public void deleteVpnUsers() throws AuthorizationDeniedException {
        if (vpnUserGuiList == null) {
            return;
        }

        String msg = null;
        try {
            for (VpnUserGuiInfo vpnUserGuiInfo : vpnUserGuiInfos) {
                if (!vpnUserGuiInfo.isSelected()){
                    continue;
                }

                // Revocation first. TODO: revocation reason parametrisation
                try {
                    endEntityManagementSession.revokeAndDeleteUser(authenticationToken, vpnUserGuiInfo.getUserDesc(), 0);
                } catch(NotFoundException e){
                    log.warn("End entity not found");
                }

                // Delete VpnUser record itself
                vpnUserManagementSession.deleteVpnUser(authenticationToken, vpnUserGuiInfo.getId());
            }
        } catch (ApprovalException | WaitingForApprovalException | RemoveException e) {
            msg = e.getMessage();
        }

        flushCaches();
        if (msg != null) {
            log.info("Message displayed to user: " + msg);
            super.addNonTranslatedErrorMessage(msg);
        }
    }

    /** Sends configuration email to the user */
    public void sendConfigEmail() {
        if (vpnUserGuiList == null) {
            return;
        }

        try {
            final VpnUserGuiInfo current = (VpnUserGuiInfo) vpnUserGuiList.getRowData();
            vpnUserManagementSession.sendConfigurationEmail(authenticationToken, current.getId(), null);

        } catch (Exception e) {
            final String msg = "Sending an email by administrator " + authenticationToken.toString() + " failed. ";
            super.addNonTranslatedErrorMessage(msg);
            log.info(msg, e);
        }
        flushCaches();
    }

    /** @return true if admin may create new or modify existing VpnUsers. */
    public boolean isAllowedToModify() {
        return accessControlSession.isAuthorizedNoLogging(authenticationToken, VpnRules.USER_MODIFY.resource());
    }
    
    /** @return true if admin may delete VpnUsers. */
    public boolean isAllowedToDelete() {
        return accessControlSession.isAuthorizedNoLogging(authenticationToken, VpnRules.USER_DELETE.resource());
    }

    /**
     * Constructs valid non-managed VpnUser from the current representation.
     * If the current user has non-null ID, the user is loaded from database,
     * changes are applied onto its clone.
     *
     * @param guiUser
     * @return detached VpnUser.
     */
    private VpnUser fromGuiUser(CurrentVpnUserGuiInfo guiUser) throws AuthorizationDeniedException, CloneNotSupportedException {
        VpnUser user = new VpnUser();
        if (guiUser.getId() != null){
            final VpnUser dbUser = vpnUserManagementSession.getVpnUser(authenticationToken, guiUser.getId());
            user = VpnUser.copy(dbUser);

        } else {
            user.setId(guiUser.getId());
        }

        user.setEmail(guiUser.getEmail());
        user.setDevice(guiUser.getDevice());
        user.setDateModified(guiUser.getDateModified());
        user.setRevokedStatus(guiUser.getRevokedStatus());
        user.setOtpDownload(guiUser.getOtpDownload());
        user.setCertificateId(guiUser.getCertificateId());
        user.setCertificate(guiUser.getCertificate());
        user.setKeyStore(guiUser.getKey());
        user.setVpnConfig(guiUser.getConfig());
        return user;
    }

    /**
     * Converts VpnUser to the user info for global listing
     * @param vpnUser
     * @return
     */
    private VpnUserGuiInfo toGuiUser(VpnUser vpnUser){
        final VpnUserGuiInfo user = new VpnUserGuiInfo();

        user.setId(vpnUser.getId());
        user.setEmail(vpnUser.getEmail());
        user.setDevice(vpnUser.getDevice());
        user.setOtpDownload(vpnUser.getOtpDownload());
        user.setDateCreated(new Date(vpnUser.getDateCreated()));
        user.setDateModified(new Date(vpnUser.getDateModified()));
        user.setRevoked(vpnUser.getRevokedStatus() > 0);
        user.setOtpUsed(dateOrNull(vpnUser.getOtpUsed()));
        user.setLastMailSent(dateOrNull(vpnUser.getLastMailSent()));
        return user;
    }

    /**
     * Creates CurrentVpnUserGuiInfo from the VpnUser entity.
     * @param vpnUser
     * @return
     */
    private CurrentVpnUserGuiInfo toCurrentGuiUser(VpnUser vpnUser){
        final CurrentVpnUserGuiInfo currentVpnUser = new CurrentVpnUserGuiInfo();
        currentVpnUser.setId(vpnUser.getId());
        currentVpnUser.setEmail(vpnUser.getEmail());
        currentVpnUser.setDevice(vpnUser.getDevice());
        currentVpnUser.setDateCreated(vpnUser.getDateCreated());
        currentVpnUser.setDateModified(vpnUser.getDateModified());
        currentVpnUser.setDateCreatedDate(new Date(vpnUser.getDateCreated()));
        currentVpnUser.setDateModifiedDate(new Date(vpnUser.getDateModified()));
        currentVpnUser.setRevokedStatus(vpnUser.getRevokedStatus());
        currentVpnUser.setOtpDownload(vpnUser.getOtpDownload());
        currentVpnUser.setCertificateId(vpnUser.getCertificateId());
        currentVpnUser.setCertificate(vpnUser.getCertificate());
        currentVpnUser.setKey(vpnUser.getKeyStore());
        currentVpnUser.setConfig(vpnUser.getVpnConfig());
        currentVpnUser.setDateOtpDownloaded(dateOrNull(vpnUser.getOtpUsed()));
        currentVpnUser.setDateMailSent(dateOrNull(vpnUser.getLastMailSent()));
        return currentVpnUser;
    }

    /**
     * Generates a new key store for the end entity, generates new VPN configuration and
     * resets OTP download.
     *
     * @param userId vpn user id
     * @param password optional password to use for end entity auth
     * @throws Exception
     */
    private VpnUser generateKeyAndConfig(int userId, Optional<String> password) throws Exception {
        return vpnUserManagementSession.newVpnCredentials(authenticationToken, userId, password, null);
    }

    /** Invoked when admin requests a VPNUser creation. */
    public void saveCurrentVpnUser() throws AuthorizationDeniedException {
        String msg = null;
        try {
            final String name = StringTools.stripUsername(getCurrentVpnUser().getName());
            final String email = getCurrentVpnUser().getEmail();
            final VpnUser vpnUser = fromGuiUser(getCurrentVpnUser());

            if (!VpnUtils.isEmailValid(email)){
                throw new IllegalArgumentException("Invalid email"); //TODO: localised message
            }

            if (!vpnUserManagementSession.isUsernameAvailable(authenticationToken, vpnUser)){
                throw new IllegalArgumentException("Name already taken"); //TODO: localised message
            }

            if (getCurrentVpnUserId() == null) {
                final int endProfileId = endEntityProfileSession.getEndEntityProfileId(VpnConfig.getClientEndEntityProfile());
                final int certProfileId = CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER;
                final CAInfo vpnCA = caSession.getCAInfo(authenticationToken, VpnConfig.getCA());
                final String dn = VpnUtils.genUserCN(name);
                final EndEntityInformation uservo = new EndEntityInformation(
                        name,
                        dn,
                        vpnCA.getCAId(),
                        VpnUtils.genUserAltName(vpnUser),
                        email,
                        EndEntityConstants.STATUS_NEW,
                        EndEntityTypes.ENDUSER.toEndEntityType(),
                        endProfileId,
                        certProfileId,
                        new Date(), new Date(),
                        SecConst.TOKEN_SOFT_P12,
                        0, null);

                // If auto-generated password is used in end entity profile, this password has to be null.
                // Password will be generated automatically and sent via email to the end entity.
                uservo.setPassword(null);
                uservo.setCardNumber(null);

                // The new auto-generated password is generated now, stored to uservo end entity.
                endEntityManagementSession.addUser(authenticationToken, uservo, false);

                // Create user itself
                VpnUser newVpnUser = null;
                try {
                    newVpnUser = vpnUserManagementSession.createVpnUser(authenticationToken, vpnUser);

                } catch(Exception e){
                    endEntityManagementSession.revokeAndDeleteUser(authenticationToken, uservo.getUsername(), 0);
                    throw new Exception("Exception in creating a new VPN user", e);
                }

                try {
                    // Create certificate
                    newVpnUser = generateKeyAndConfig(newVpnUser.getId(), Optional.ofNullable(uservo.getPassword()));

                    // Send an email.
                    if (getCurrentVpnUser().isSendConfigEmail()) {
                        vpnUserManagementSession.sendConfigurationEmail(authenticationToken, newVpnUser.getId(), null);
                    }

                } catch (Exception e) {
                    // If things went wrong set status to FAILED
                    final String newStatusString;
                    endEntityManagementSession.setUserStatus(authenticationToken, uservo.getUsername(),
                            EndEntityConstants.STATUS_FAILED);
                    newStatusString = "FAILED";

                    if (e instanceof IllegalKeyException) {
                        final String errMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.erroruser", uservo.getUsername());
                        log.error(errMsg + " " + e.getMessage());
                        log.error(InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.errorsetstatus", newStatusString));
                        log.error(InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.errorcheckconfig"));
                    } else {
                        log.error(InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.errorsetstatus", newStatusString), e);
                        final String errMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.erroruser", uservo.getUsername());
                        throw new Exception(errMsg, e);
                    }
                }

                currentVpnUserId = newVpnUser.getId();
                msg = "VpnUser created successfully.";

            } else {
                vpnUserManagementSession.saveVpnUser(authenticationToken, vpnUser);
                msg = "VpnUser saved successfully.";
            }

            setCurrentVpnUserEditMode(false);

        } catch (AuthorizationDeniedException e) {
            msg = e.getMessage();
        } catch (IllegalArgumentException e) {
            msg = e.getMessage();
        } catch (Throwable e) {
            msg = e.getMessage();
            log.info("General exception in saving the user", e);
        }

        flushCaches();
        if (msg != null) {
            log.info("Message displayed to user: " + msg);
            super.addNonTranslatedErrorMessage(msg);
        }
    }

    /** Invoked when admin cancels a VpnUser create or edit. */
    public void cancelEdit() {
        setCurrentVpnUserEditMode(false);
        flushCaches();
    }

    /** Used to draw the back link. No white-listing to the calling method must be careful to only use this for branching. */
    public String getParamRef() {
        final String reference = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("ref");
        if (reference==null || reference.isEmpty()) {
            return "default";
        }
        return reference;
    }
    
    /** @return the id of the VPNUser that is subject to view or edit */
    public Integer getCurrentVpnUserId() {
        // Get the HTTP GET/POST parameter named "vpnUserId"
        String vpnUserIdString = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("vpnUserId");

        if (vpnUserIdString!=null) {
            if (vpnUserIdString.isEmpty()){
                this.currentVpnUserId = null;
            } else {
                try {
                    int vpnUserId = Integer.parseInt(vpnUserIdString);
                    // If there is a query parameter present and the id is different we flush the cache!
                    if (this.currentVpnUserId == null || vpnUserId != this.currentVpnUserId) {
                        flushCaches();
                        this.currentVpnUserId = vpnUserId;
                    }

                } catch (NumberFormatException e) {
                    log.info("Bad 'vpnUserId' parameter value.. set, but not a number..");
                }
            }

            // Always switch to edit mode for new ones and view mode for all others
            setCurrentVpnUserEditMode(this.currentVpnUserId == null);
        }

        return currentVpnUserId;
    }

    /** @return cached or populate a new CryptoToken GUI representation for view or edit */
    public CurrentVpnUserGuiInfo getCurrentVpnUser() throws AuthorizationDeniedException {
        if (this.currentVpnUser == null) {
            final Integer vpnUserId = getCurrentVpnUserId();
            this.currentVpnUser = new CurrentVpnUserGuiInfo();

            // If the id is non-zero we try to load an existing user
            if (vpnUserId != null) {
                final VpnUser vpnUser = vpnUserManagementSession.getVpnUser(authenticationToken, vpnUserId);
                if (vpnUser == null) {
                    throw new RuntimeException("Could not load VpnUser with vpnUserId " + vpnUserId);
                } else {
                    this.currentVpnUser = toCurrentGuiUser(vpnUser);
                }
            }
        }
        return this.currentVpnUser;
    }

    public boolean isCurrentVpnUserEditMode() {
        return currentVpnUserEditMode;
    }

    public void setCurrentVpnUserEditMode(boolean currentVpnUserEditMode) {
        this.currentVpnUserEditMode = currentVpnUserEditMode;
    }

    public void toggleCurrentCryptoTokenEditMode() {
        currentVpnUserEditMode ^= true;
    }

}
