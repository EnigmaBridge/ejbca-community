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
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.certificate.IllegalKeyException;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileSession;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.certificates.util.AlgorithmTools;
import org.cesecore.keybind.InternalKeyBindingMgmtSessionLocal;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.util.CertTools;
import org.cesecore.vpn.VpnUserManagementSession;
import org.cesecore.vpn.VpnUser;
import org.ejbca.core.ejb.ca.auth.EndEntityAuthenticationSession;
import org.ejbca.core.ejb.ca.sign.SignSession;
import org.ejbca.core.ejb.ra.*;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionLocal;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.ui.web.admin.BaseManagedBean;
import org.ejbca.ui.web.admin.rainterface.RAInterfaceBean;
import org.ejbca.ui.web.admin.rainterface.UserView;

import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import java.io.IOException;
import java.io.Serializable;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * JavaServer Faces Managed Bean for managing VPN users.
 * Session scoped and will cache the list of tokens and keys.
 * 
 * @version $Id: VpnUsersMBean.java 20320 2014-11-25 18:05:45Z mikekushner $
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
        private boolean revoked;
        private Certificate certificate;
        private PrivateKey key;
        private boolean selected = false;

        // End entity view
        private UserView userview;
        private String statusText;

        public VpnUserGuiInfo() {
        }

        public VpnUserGuiInfo(Integer id, String email, String device, Date dateCreated, Date dateModified, boolean revoked, Certificate certificate) {
            this.id = id;
            this.email = email;
            this.device = device;
            this.dateCreated = dateCreated;
            this.dateModified = dateModified;
            this.revoked = revoked;
            this.certificate = certificate;
            regenerateId();
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
    }

    /** GUI edit/view representation of a VpnUser that can be interacted with. */
    public class CurrentVpnUserGuiInfo {
        private Integer id;
        private String name = "";
        private String email = "";
        private String device = "default";
        private boolean active = false;
        private boolean referenced = false;

        private long dateCreated;
        private long dateModified;
        private int revokedStatus;
        private String otpDownload;
        private String certificateId;
        private String certificate;
        private String key;
        private String config;

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

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isReferenced() {
            return referenced;
        }

        public void setReferenced(boolean referenced) {
            this.referenced = referenced;
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
    }

    private List<VpnUserGuiInfo> vpnUserGuiInfos = new ArrayList<VpnUserGuiInfo>();

    @SuppressWarnings("rawtypes") //JDK6 does not support typing for ListDataModel
    private ListDataModel vpnUserGuiList = null;

    @SuppressWarnings("rawtypes") //JDK6 does not support typing for ListDataModel
    private ListDataModel keyPairGuiList = null;
    private String keyPairGuiListError = null;
    private Integer currentVpnUserId = null;
    private CurrentVpnUserGuiInfo currentVpnUser = null;
    private boolean currentVpnUserEditMode = true;  // currentVpnUserId==0 from start

    private final VpnUserManagementSession vpnUserManagementSession = getEjbcaWebBean().getEjb().getVpnUserManagementSession();
    private final EndEntityProfileSessionLocal endEntityProfileSession = getEjbcaWebBean().getEjb().getEndEntityProfileSession();
    private final CertificateProfileSession certificateProfileSession = getEjbcaWebBean().getEjb().getCertificateProfileSession();
    private final EndEntityManagementSession endEntityManagementSession = getEjbcaWebBean().getEjb().getEndEntityManagementSession();
    private final EndEntityAccessSession endEntityAccessSession = getEjbcaWebBean().getEjb().getEndEntityAccessSession();
    private final EndEntityAuthenticationSession endEntityAuthenticationSession = getEjbcaWebBean().getEjb().getEndEntityAuthenticationSession();
    private final SignSession signSession = getEjbcaWebBean().getEjb().getSignSession();

    private final AccessControlSessionLocal accessControlSession = getEjbcaWebBean().getEjb().getAccessControlSession();
    private final AuthenticationToken authenticationToken = getAdmin();
    private final CaSessionLocal caSession = getEjbcaWebBean().getEjb().getCaSession();
    private final InternalKeyBindingMgmtSessionLocal internalKeyBindingMgmtSession = getEjbcaWebBean().getEjb().getInternalKeyBindingMgmtSession();

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
        keyPairGuiList = null;
        currentVpnUser = null;
    }

    /**
     * Gets RA bean.
     * @return
     */
    private RAInterfaceBean getRaif() throws IOException, ClassNotFoundException {
        if (raif == null) {
           raif = VpnUtils.getRaBean();
        }
        return raif;
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

    /** Build a list sorted by name from the authorized cryptoTokens that can be presented to the user */
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

    /** Invoked when admin requests a new VpnUser credentials, revoking the old ones. */
    public void regenerateVpnUser() throws AuthorizationDeniedException {
        // TODO: implement
        if (vpnUserGuiList !=null) {
//            final VpnUserGuiInfo current = (VpnUserGuiInfo) vpnUserGuiList.getRowData();
//            try {
//                vpnUserManagementSession.activate(authenticationToken, current.getCryptoTokenId(), current.getAuthenticationCode().toCharArray());
//            } catch (CryptoTokenOfflineException e) {
//                final String msg = "Activation of CryptoToken '" + current.getTokenName() + "' (" + current.getCryptoTokenId() +
//                        ") by administrator " + authenticationToken.toString() + " failed. Device was unavailable.";
//                super.addNonTranslatedErrorMessage(msg);
//                log.info(msg);
//            } catch (CryptoTokenAuthenticationFailedException e) {
//                final String msg = "Activation of CryptoToken '" + current.getTokenName() + "' (" + current.getCryptoTokenId() +
//                        ") by administrator " + authenticationToken.toString() + " failed. Authentication code was not correct.";
//                super.addNonTranslatedErrorMessage(msg);
//                log.info(msg);
//            }
            flushCaches();
        }
    }

    /** Invoked when admin requests a CryptoToken deactivation. */
    public void revokeVpnUser() throws AuthorizationDeniedException {
        // TODO: implement
        if (vpnUserGuiList !=null) {
            final VpnUserGuiInfo rowData = (VpnUserGuiInfo) vpnUserGuiList.getRowData();
//            vpnUserManagementSession.deactivate(authenticationToken, rowData.getCryptoTokenId());
            flushCaches();
        }
    }
    
    /** Invoked when admin requests a CryptoToken deletion. */
    public void deleteVpnUser() throws AuthorizationDeniedException {
        if (vpnUserGuiList !=null) {
            // TODO: revoke certificate.
            final VpnUserGuiInfo rowData = (VpnUserGuiInfo) vpnUserGuiList.getRowData();
            vpnUserManagementSession.deleteVpnUser(authenticationToken, rowData.getId());
            flushCaches();
        }
    }

    /** @return true if admin may create new or modify existing CryptoTokens. */
    public boolean isAllowedToModify() {
        //TODO: Migrate to VPN user auth
        return accessControlSession.isAuthorizedNoLogging(authenticationToken, CryptoTokenRules.MODIFY_CRYPTOTOKEN.resource());
    }
    
    /** @return true if admin may delete CryptoTokens. */
    public boolean isAllowedToDelete() {
        //TODO: Migrate to VPN user auth
        return accessControlSession.isAuthorizedNoLogging(authenticationToken, CryptoTokenRules.DELETE_CRYPTOTOKEN.resource());
    }

    private VpnUser fromGuiUser(CurrentVpnUserGuiInfo guiUser){
        final VpnUser user = new VpnUser();

        user.setId(guiUser.getId());
        user.setEmail(guiUser.getName());
        user.setDevice(guiUser.getDevice());
        user.setDateModified(guiUser.getDateModified());
        user.setRevokedStatus(guiUser.getRevokedStatus());
        user.setOtpDownload(guiUser.getOtpDownload());
        user.setCertificateId(guiUser.getCertificateId());
        user.setCertificate(guiUser.getCertificate());
        user.setPrivKey(guiUser.getKey());
        user.setVpnConfig(guiUser.getConfig());

        return user;
    }

    private VpnUserGuiInfo toGuiUser(VpnUser vpnUser){
        final VpnUserGuiInfo user = new VpnUserGuiInfo();

        user.setId(vpnUser.getId());
        user.setEmail(vpnUser.getEmail());
        user.setDevice(vpnUser.getDevice());
        user.setDateCreated(new Date(vpnUser.getDateCreated()));
        user.setDateModified(new Date(vpnUser.getDateModified()));
        user.setRevoked(vpnUser.getRevokedStatus() > 0);

        return user;
    }

    /** Invoked when admin requests a CryptoToken creation. */
    public void saveCurrentVpnUser() throws AuthorizationDeniedException {
        String msg = null;
        try {
            final Properties properties = new Properties();
            final String name = getCurrentVpnUser().getName();
            final String email = getCurrentVpnUser().getEmail();
            final String device = getCurrentVpnUser().getDevice();
            final VpnUser vpnUser = fromGuiUser(getCurrentVpnUser());

            if (getCurrentVpnUserId() == null) {
                // End profile
                final EndEntityProfile endProfile = endEntityProfileSession.getEndEntityProfile("VPN"); // TODO: to config
                final int endProfileId = endEntityProfileSession.getEndEntityProfileId("VPN"); // TODO: to config

                // Certificate profile
                final int certProfileId = CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER;

                // Get CA that works with VPN.
                final CA vpnCA = caSession.getCA(authenticationToken, "VPN"); // TODO: to config

                // Create new end entity.
                UserView uview = new UserView();
                uview.setCAId(vpnCA.getCAId());
                uview.setEndEntityProfileId(endProfileId);
                uview.setCertificateProfileId(certProfileId);

                uview.setEmail(email);
                uview.setUsername(name);
                uview.setTimeCreated(new Date());
                uview.setTimeModified(new Date());

                uview.setSubjectDN("CN="+name); // TODO: sanitize
                uview.setClearTextPassword(false);

                // If auto-generated password is used in end entity profile, this password has to be null.
                // Password will be generated automatically and sent via email to the end entity.
                uview.setPassword(null); // TODO: random password
                uview.setTokenType(SecConst.TOKEN_SOFT_P12);

                //raif.addUser(uview);
                EndEntityInformation uservo = new EndEntityInformation(uview.getUsername(), uview.getSubjectDN(),
                        uview.getCAId(), uview.getSubjectAltName(),
                        uview.getEmail(), EndEntityConstants.STATUS_NEW, uview.getType(),
                        uview.getEndEntityProfileId(), uview.getCertificateProfileId(),
                        null,null, uview.getTokenType(),
                        uview.getHardTokenIssuerId(), null);

                uservo.setPassword(uview.getPassword());
                uservo.setExtendedinformation(uview.getExtendedInformation());
                uservo.setCardNumber(uview.getCardNumber());
                endEntityManagementSession.addUser(authenticationToken, uservo, uview.getClearTextPassword());

                // Create certificate
                try {
                    doCreateKeys(uservo, EndEntityConstants.STATUS_NEW);

                } catch (Exception e) {
                    // If things went wrong set status to FAILED
                    final String newStatusString;
                    endEntityManagementSession.setUserStatus(authenticationToken, uservo.getUsername(),
                            EndEntityConstants.STATUS_FAILED);
                    newStatusString = "FAILED";

                    if (e instanceof IllegalKeyException) {
                        final String errMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("batch.errorbatchfaileduser", uservo.getUsername());
                        log.error(errMsg + " " + e.getMessage());
                        log.error(InternalEjbcaResources.getInstance().getLocalizedMessage("batch.errorsetstatus", newStatusString));
                        log.error(InternalEjbcaResources.getInstance().getLocalizedMessage("batch.errorcheckconfig"));
                    } else {
                        log.error(InternalEjbcaResources.getInstance().getLocalizedMessage("batch.errorsetstatus", newStatusString), e);
                        final String errMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("batch.errorbatchfaileduser", uservo.getUsername());
                        throw new Exception(errMsg);
                    }
                }

                // Create user itself
                final VpnUser newVpnUser = vpnUserManagementSession.createVpnUser(authenticationToken, vpnUser);
                currentVpnUserId = newVpnUser.getId();
                msg = "VpnUser created successfully.";

            } else {
                vpnUserManagementSession.saveVpnUser(authenticationToken, vpnUser);
                msg = "VpnUser saved successfully.";
            }

            flushCaches();
            setCurrentVpnUserEditMode(false);

        } catch (AuthorizationDeniedException e) {
            msg = e.getMessage();
        } catch (IllegalArgumentException e) {
            msg = e.getMessage();
        } catch (Throwable e) {
            msg = e.getMessage();
            log.info("", e);
        }

        if (msg != null) {
            log.info("Message displayed to user: " + msg);
            super.addNonTranslatedErrorMessage(msg);
        }
    }

    /**
     * Generates a new RSA keys & certificate for the end entity.
     * @param data EndEntityInformation
     * @param status entity status to determine a) generate new key b) recover stored key
     * @return
     * @throws Exception
     */
    private KeyStore doCreateKeys(EndEntityInformation data, int status) throws Exception {
        KeyStore ret = null;

        // get users Token Type.
        int tokentype = data.getTokenType();
        boolean createJKS = (tokentype == SecConst.TOKEN_SOFT_JKS);
        boolean createPEM = (tokentype == SecConst.TOKEN_SOFT_PEM);
        boolean createP12 = (tokentype == SecConst.TOKEN_SOFT_P12);
        // Only generate supported tokens
        if (createP12 || createPEM || createJKS) {
//            if (status == EndEntityConstants.STATUS_KEYRECOVERY) {
//                String iMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("batch.retrieveingkeys", data.getEmail());
//                log.info(iMsg);
//            } else {
//                String iMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("batch.generatingkeys", getProps().getKeyAlg(),
//                        getProps().getKeySpec(), data.getEmail());
//                log.info(iMsg);
//            }

            KeyStore ks = processUser(data, createJKS, createPEM, (status == EndEntityConstants.STATUS_KEYRECOVERY));

            // If all was OK, users status is set to GENERATED by the
            // signsession when the user certificate is created.
            // If status is still NEW, FAILED or KEYRECOVER though, it means we
            // should set it back to what it was before, probably it had a
            // request counter
            // meaning that we should not reset the clear text password yet.
            EndEntityInformation vo = endEntityAccessSession.findUser(authenticationToken, data.getUsername());

            if ((vo.getStatus() == EndEntityConstants.STATUS_NEW) || (vo.getStatus() == EndEntityConstants.STATUS_FAILED)
                    || (vo.getStatus() == EndEntityConstants.STATUS_KEYRECOVERY)) {
                endEntityManagementSession.setClearTextPassword(authenticationToken, data.getUsername(), data.getPassword());
            } else {
                // Delete clear text password, if we are not letting status be
                // the same as originally
                endEntityManagementSession.setClearTextPassword(authenticationToken, data.getUsername(), null);
            }
            ret = ks;
            String iMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("batch.generateduser", data.getUsername());
            log.info(iMsg);
        } else {
            log.error("Cannot batchmake browser generated token for user (wrong tokentype)- " + data.getUsername());
        }
        return ret;
    }

    /**
     * Recovers or generates new keys for the user and generates keystore
     * From: BatchMakeP12Command.java
     *
     * @param data
     *            user data for user
     * @param createJKS
     *            if a jks should be created
     * @param createPEM
     *            if pem files should be created
     * @param keyrecoverflag
     *            if we should try to revoer already existing keys
     * @throws Exception
     *             If something goes wrong...
     */
    private KeyStore processUser(EndEntityInformation data, boolean createJKS, boolean createPEM, boolean keyrecoverflag)
            throws Exception {

        X509Certificate orgCert = null;
        KeyPair rsaKeys = KeyTools.genKeys("2048", AlgorithmConstants.KEYALGORITHM_RSA);

        // Get certificate for user and create keystore
        if (rsaKeys != null) {
            return createKeysForUser(data.getUsername(), data.getPassword(), data.getCAId(), rsaKeys, createJKS, createPEM,
                    !keyrecoverflag && data.getKeyRecoverable(), orgCert);
        }

        return null;
    }

    /**
     * Creates files for a user, sends request to CA, receives reply and creates
     * P12.
     *
     * @param username
     *            username
     * @param password
     *            user's password
     * @param caid
     *            of CA used to issue the keystore certificates
     * @param rsaKeys
     *            a previously generated RSA keypair
     * @param createJKS
     *            if a jks should be created
     * @param createPEM
     *            if pem files should be created
     * @param savekeys
     *            if generated keys should be saved in db (key recovery)
     * @param orgCert
     *            if an original key recovered cert should be reused, null
     *            indicates generate new cert.
     * @throws Exception
     *             if the certificate is not an X509 certificate
     * @throws Exception
     *             if the CA-certificate is corrupt
     * @throws Exception
     *             if verification of certificate or CA-cert fails
     * @throws Exception
     *             if keyfile (generated by ourselves) is corrupt
     */

    private KeyStore createKeysForUser(String username, String password, int caid, KeyPair rsaKeys, boolean createJKS,
                                   boolean createPEM, boolean savekeys, X509Certificate orgCert) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(">createKeysForUser: username=" + username);
        }

        X509Certificate cert = null;

        if (orgCert != null) {
            cert = orgCert;
            boolean finishUser = caSession.getCAInfo(authenticationToken, caid).getFinishUser();
            if (finishUser) {
                EndEntityInformation userdata = endEntityAccessSession.findUser(authenticationToken, username);
                endEntityAuthenticationSession.finishUser(userdata);
            }

        } else {
            String sigAlg = AlgorithmConstants.SIGALG_SHA1_WITH_RSA;
            X509Certificate selfcert = CertTools.genSelfCert("CN=selfsigned", 1, null, rsaKeys.getPrivate(), rsaKeys.getPublic(), sigAlg, false);
            cert = (X509Certificate) signSession.createCertificate(authenticationToken, username, password, selfcert);
        }

        // Make a certificate chain from the certificate and the CA-certificate
        Certificate[] cachain = signSession.getCertificateChain(authenticationToken, caid).toArray(new Certificate[0]);
        // Verify CA-certificate
        if (CertTools.isSelfSigned((X509Certificate) cachain[cachain.length - 1])) {
            try {
                // Make sure we have BC certs, otherwise SHA256WithRSAAndMGF1
                // will not verify (at least not as of jdk6)
                Certificate cacert = CertTools.getCertfromByteArray(cachain[cachain.length - 1].getEncoded());
                cacert.verify(cacert.getPublicKey());

            } catch (GeneralSecurityException se) {
                String errMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("batch.errorrootnotverify");
                throw new Exception(errMsg);
            }
        } else {
            String errMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("batch.errorrootnotselfsigned");
            throw new Exception(errMsg);
        }

        // Verify that the user-certificate is signed by our CA
        try {
            // Make sure we have BC certs, otherwise SHA256WithRSAAndMGF1 will
            // not verify (at least not as of jdk6)
            Certificate cacert = CertTools.getCertfromByteArray(cachain[0].getEncoded());
            Certificate usercert = CertTools.getCertfromByteArray(cert.getEncoded());
            usercert.verify(cacert.getPublicKey());
        } catch (GeneralSecurityException se) {
            String errMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("batch.errorgennotverify");
            throw new Exception(errMsg);
        }

        // Use CN if as alias in the keystore, if CN is not present use username
        String alias = CertTools.getPartFromDN(CertTools.getSubjectDN(cert), "CN");
        if (alias == null) {
            alias = username;
        }

        // Store keys and certificates in keystore.
        KeyStore ks = null;

        if (createJKS) {
            ks = KeyTools.createJKS(alias, rsaKeys.getPrivate(), password, cert, cachain);
        } else {
            ks = KeyTools.createP12(alias, rsaKeys.getPrivate(), cert, cachain);
        }

        storeKeyStore(ks, username, password, createJKS, createPEM);
        String iMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("batch.createkeystore", username);
        log.info(iMsg);
        if (log.isTraceEnabled()) {
            log.trace("<createUser: username=" + username);
        }

        return ks;
    }

    /**
     * Stores keystore.
     *
     * @param ks
     *            KeyStore
     * @param username
     *            username, the owner of the keystore
     * @param kspassword
     *            the password used to protect the peystore
     * @param createJKS
     *            if a jks should be created
     * @param createPEM
     *            if pem files should be created
     * @throws IOException
     *             if directory to store keystore cannot be created
     */
    private void storeKeyStore(KeyStore ks, String username, String kspassword, boolean createJKS, boolean createPEM) throws IOException,
            KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException {
        if (log.isTraceEnabled()) {
            log.trace(">storeKeyStore: ks=" + ks.toString() + ", username=" + username);
        }

//        // Where to store it?
//        if (mainStoreDir == null) {
//            throw new IOException("Can't find directory to store keystore in.");
//        }
//
//        if (!new File(mainStoreDir).exists()) {
//            new File(mainStoreDir).mkdir();
//            log.info("Directory '" + mainStoreDir + "' did not exist and was created.");
//        }
//
//        String keyStoreFilename = mainStoreDir + "/" + username;
//
//        if (createJKS) {
//            keyStoreFilename += ".jks";
//        } else {
//            keyStoreFilename += ".p12";
//        }
//
//        // If we should also create PEM-files, do that
//        if (createPEM) {
//            String PEMfilename = mainStoreDir + "/pem";
//            P12toPEM p12topem = new P12toPEM(ks, kspassword, true);
//            p12topem.setExportPath(PEMfilename);
//            p12topem.createPEM();
//        } else {
//            FileOutputStream os = new FileOutputStream(keyStoreFilename);
//            ks.store(os, kspassword.toCharArray());
//        }
//
//        log.debug("Keystore stored in " + keyStoreFilename);
        if (log.isTraceEnabled()) {
            log.trace("<storeKeyStore: ks=" + ks.toString() + ", username=" + username);
        }
    }

    /** Invoked when admin cancels a CryptoToken create or edit. */
    public void cancelCurrentCryptoToken() {
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
                    log.info("Bad 'cryptoTokenId' parameter value.. set, but not a number..");
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
            final CurrentVpnUserGuiInfo currentVpnUser = new CurrentVpnUserGuiInfo();
            // If the id is non-zero we try to load an existing token
            if (vpnUserId!=null) {
                final VpnUser vpnUser = vpnUserManagementSession.getVpnUser(authenticationToken, vpnUserId);
                if (vpnUser == null) {
                    throw new RuntimeException("Could not load VpnUser with vpnUserId " + vpnUserId);
                } else {
                    currentVpnUser.setId(vpnUser.getId());
                    currentVpnUser.setEmail(vpnUser.getEmail());
                    currentVpnUser.setDevice(vpnUser.getDevice());
                    currentVpnUser.setDateCreated(vpnUser.getDateCreated());
                    currentVpnUser.setDateModified(vpnUser.getDateModified());
                    currentVpnUser.setRevokedStatus(vpnUser.getRevokedStatus());
                    currentVpnUser.setOtpDownload(vpnUser.getOtpDownload());
                    currentVpnUser.setCertificateId(vpnUser.getCertificateId());
                    currentVpnUser.setCertificate(vpnUser.getCertificate());
                    currentVpnUser.setKey(vpnUser.getPrivKey());
                    currentVpnUser.setConfig(vpnUser.getVpnConfig());
                    currentVpnUser.setActive(true);
                    currentVpnUser.setReferenced(true);
                }
            }
            this.currentVpnUser = currentVpnUser;
        }
        return this.currentVpnUser;
    }
    
    public void selectCryptoTokenType() {
        // NOOP: Only for page reload
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
    
    //
    // KeyPair related stuff
    //
    
    // This default is taken from CAToken.SOFTPRIVATESIGNKEYALIAS, but we don't want to depend on the CA module
    private String newKeyPairAlias = "privatesignkeyalias";
    private String newKeyPairSpec = AlgorithmConstants.KEYALGORITHM_RSA+"4096";
    
    /** @return a List of available (but not neccessarly supported by the underlying CryptoToken) key specs */
    public List<SelectItem> getAvailbleKeySpecs() {
//        final List<SelectItem> availableKeySpecs = new ArrayList<SelectItem>();
//        final int[] SIZES_RSA = {1024, 1536, 2048, 3072, 4096, 6144, 8192};
//        final int[] SIZES_DSA = {1024};
//        for (int size : SIZES_RSA) {
//            availableKeySpecs.add(new SelectItem(AlgorithmConstants.KEYALGORITHM_RSA+size, AlgorithmConstants.KEYALGORITHM_RSA+" "+size));
//        }
//        for (int size : SIZES_DSA) {
//            availableKeySpecs.add(new SelectItem(AlgorithmConstants.KEYALGORITHM_DSA+size, AlgorithmConstants.KEYALGORITHM_DSA+" "+size));
//        }
//        final Map<String,String> processedCurveNames = new HashMap<String,String>();
//        @SuppressWarnings("unchecked")
//        final Enumeration<String> ecNamedCurves = ECNamedCurveTable.getNames();
//        while (ecNamedCurves.hasMoreElements()) {
//            final String ecNamedCurve = ecNamedCurves.nextElement();
//            // Only add it if the key-length is sufficient
//            try {
//                final ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec(ecNamedCurve);
//                final int bitLength = parameterSpec.getN().bitLength();
//                KeyTools.checkValidKeyLength(AlgorithmConstants.KEYALGORITHM_ECDSA, bitLength);
//                // Check if this exists under another alias
//                boolean added = false;
//                for (final String name : processedCurveNames.keySet()) {
//                    final ECNamedCurveParameterSpec parameterSpec2 = ECNamedCurveTable.getParameterSpec(name);
//                    if (parameterSpec.equals(parameterSpec2)) {
//                        // We have already listed this curve under another name
//                        added = true;
//                        break;
//                    }
//                }
//                if (!added) {
//                    if (PKCS11CryptoToken.class.getSimpleName().equals(getCurrentVpnUser().getType())) {
//                        if (AlgorithmTools.isNamedECKnownInDefaultProvider(ecNamedCurve)) {
//                            processedCurveNames.put(ecNamedCurve, getEcKeySpecAliases(ecNamedCurve));
//                        }
//                    } else {
//                        processedCurveNames.put(ecNamedCurve, getEcKeySpecAliases(ecNamedCurve));
//                    }
//                }
//            } catch (InvalidKeyException e) {
//                // Ignore very silently
//                if (log.isTraceEnabled()) {
//                    log.trace("Not adding keys that are not allowed to key list: "+e.getMessage());
//                }
//            } catch (Exception e) {
//                // Ignore
//                if (log.isDebugEnabled()) {
//                    log.debug(e);
//                }
//            }
//        }
//        String[] keys = processedCurveNames.keySet().toArray(new String[0]);
//        Arrays.sort(keys, new Comparator<String>() {
//            @Override
//            public int compare(String o1, String o2) {
//                return o1.compareTo(o2);
//            }
//        });
//        for (String name : keys) {
//            availableKeySpecs.add(new SelectItem(name, AlgorithmConstants.KEYALGORITHM_ECDSA + " "+processedCurveNames.get(name)));
//        }
//
//        for (String alg : CesecoreConfiguration.getExtraAlgs()) {
//            for (String subalg : CesecoreConfiguration.getExtraAlgSubAlgs(alg)) {
//                final String title = CesecoreConfiguration.getExtraAlgSubAlgTitle(alg, subalg);
//                final String name = CesecoreConfiguration.getExtraAlgSubAlgName(alg, subalg);
//                availableKeySpecs.add(new SelectItem(name, title));
//            }
//        }
//
//        return availableKeySpecs;
        return null;
    }

    private String getEcKeySpecAliases(final String ecKeySpec) {
        StringBuilder ret = new StringBuilder();
        for (final String alias : AlgorithmTools.getEcKeySpecAliases(ecKeySpec)) {
            if (ret.length()!=0) {
                ret.append(" / ");
            }
            ret.append(alias);
        }
        return ret.toString();
    }

    /** @return true if admin may generate keys in the current CryptoTokens. */
    public boolean isAllowedToKeyGeneration() {
        return accessControlSession.isAuthorizedNoLogging(authenticationToken, CryptoTokenRules.GENERATE_KEYS.resource() + '/' + getCurrentVpnUserId());
    }

    /** @return true if admin may test keys from the current CryptoTokens. */
    public boolean isAllowedToKeyTest() {
        return accessControlSession.isAuthorizedNoLogging(authenticationToken, CryptoTokenRules.TEST_KEYS.resource() + '/' + getCurrentVpnUserId());
    }

    /** @return true if admin may remove keys from the current CryptoTokens. */
    public boolean isAllowedToKeyRemoval() {
        return accessControlSession.isAuthorizedNoLogging(authenticationToken, CryptoTokenRules.REMOVE_KEYS.resource() + '/' + getCurrentVpnUserId());
    }

    public boolean isKeyPairGuiListEmpty() throws AuthorizationDeniedException {
        return getKeyPairGuiList().getRowCount()==0;
    }
    
    public boolean isKeyPairGuiListFailed() throws AuthorizationDeniedException {
        getKeyPairGuiList(); // ensure loaded
        return keyPairGuiListError!=null;
    }
    
    public String getKeyPairGuiListError() throws AuthorizationDeniedException {
        getKeyPairGuiList(); // ensure loaded
        return keyPairGuiListError;
    }
    
    /** @return a list of all the keys in the current CryptoToken. */
    @SuppressWarnings({ "rawtypes", "unchecked" }) //JDK6 does not support typing for ListDataModel
    public ListDataModel getKeyPairGuiList() throws AuthorizationDeniedException {
        if (keyPairGuiList==null) {
//            final List<KeyPairGuiInfo> ret = new ArrayList<KeyPairGuiInfo>();
//            if (getCurrentVpnUser().isActive()) {
//                // Add existing key pairs
//                try {
//                    for (KeyPairInfo keyPairInfo : vpnUserManagementSession.getKeyPairInfos(getAdmin(), getCurrentVpnUserId())) {
//                        ret.add(new KeyPairGuiInfo(keyPairInfo));
//                    }
//                } catch (CryptoTokenOfflineException ctoe) {
//                    keyPairGuiListError = "Failed to load key pairs from CryptoToken: "+ctoe.getMessage();
//                }
//                // Add placeholders for key pairs
//                String keyPlaceholders = getCurrentVpnUser().getKeyPlaceholders();
//                for (String template : keyPlaceholders.split("["+CryptoToken.KEYPLACEHOLDERS_OUTER_SEPARATOR+"]")) {
//                    if (!template.trim().isEmpty()) {
//                        ret.add(new KeyPairGuiInfo(template));
//                    }
//                }
//            }
//            Collections.sort(ret, new Comparator<KeyPairGuiInfo>() {
//                @Override
//                public int compare(KeyPairGuiInfo keyPairInfo1, KeyPairGuiInfo keyPairInfo2) {
//                    return keyPairInfo1.getAlias().compareTo(keyPairInfo2.getAlias());
//                }
//            });
//            keyPairGuiInfos = ret;
//            keyPairGuiList = new ListDataModel(keyPairGuiInfos);
        }
        return keyPairGuiList;
    }

    public String getNewKeyPairSpec() { return newKeyPairSpec; }
    public void setNewKeyPairSpec(String newKeyPairSpec) { this.newKeyPairSpec = newKeyPairSpec; }

    public String getNewKeyPairAlias() { return newKeyPairAlias; }
    public void setNewKeyPairAlias(String newKeyPairAlias) { this.newKeyPairAlias = newKeyPairAlias; }

//    /** Invoked when admin requests a new key pair generation. */
//    public void generateNewKeyPair() {
//        log.info(">generateNewKeyPair");
//        try {
//            vpnUserManagementSession.createKeyPair(getAdmin(), getCurrentVpnUserId(), getNewKeyPairAlias(), getNewKeyPairSpec());
//        } catch (CryptoTokenOfflineException e) {
//            super.addNonTranslatedErrorMessage("Token is off-line. KeyPair cannot be generated.");
//        } catch (Exception e) {
//            super.addNonTranslatedErrorMessage(e.getMessage());
//            final String logMsg = getAdmin().toString() + " failed to generate a keypair:";
//            if (log.isDebugEnabled()) {
//                log.debug(logMsg, e);
//            } else {
//                log.info(logMsg + e.getMessage());
//            }
//        }
//        flushCaches();
//        log.info("<generateNewKeyPair");
//    }
    
    /** Invoked when admin requests key pair generation from a template placeholder */
    public void generateFromTemplate() {
        log.info(">generateFromTemplate");
//        final KeyPairGuiInfo keyPairGuiInfo = (KeyPairGuiInfo) keyPairGuiList.getRowData();
//        final String alias = keyPairGuiInfo.getAlias();
//        final String keyspec = KeyTools.keyalgspecToKeyspec(keyPairGuiInfo.getKeyAlgorithm(), keyPairGuiInfo.getRawKeySpec());
//        try {
//            vpnUserManagementSession.createKeyPairFromTemplate(getAdmin(), getCurrentVpnUserId(), alias, keyspec);
//        } catch (CryptoTokenOfflineException e) {
//            super.addNonTranslatedErrorMessage("Token is off-line. KeyPair cannot be generated.");
//        } catch (Exception e) {
//            super.addNonTranslatedErrorMessage(e.getMessage());
//            final String logMsg = getAdmin().toString() + " failed to generate a keypair:";
//            if (log.isDebugEnabled()) {
//                log.debug(logMsg, e);
//            } else {
//                log.info(logMsg + e.getMessage());
//            }
//        }
        flushCaches();
        log.info("<generateFromTemplate");
    }
    
    /** Invoked when admin requests a test of a key pair. */
    public void testKeyPair() {
//        final KeyPairGuiInfo keyPairGuiInfo = (KeyPairGuiInfo) keyPairGuiList.getRowData();
//        final String alias = keyPairGuiInfo.getAlias();
//        try {
//            vpnUserManagementSession.testKeyPair(getAdmin(), getCurrentVpnUserId(), alias);
//            super.addNonTranslatedInfoMessage(alias + " tested successfully.");
//        } catch (Exception e) {
//            super.addNonTranslatedErrorMessage(e.getMessage());
//        }
    }
    
    /** Invoked when admin requests the removal of a key pair. */
    public void removeKeyPair() {
//        final KeyPairGuiInfo keyPairGuiInfo = (KeyPairGuiInfo) keyPairGuiList.getRowData();
//        final String alias = keyPairGuiInfo.getAlias();
//        try {
//            if (!keyPairGuiInfo.isPlaceholder()) {
//                vpnUserManagementSession.removeKeyPair(getAdmin(), getCurrentVpnUserId(), alias);
//            } else {
//                vpnUserManagementSession.removeKeyPairPlaceholder(getAdmin(), getCurrentVpnUserId(), alias);
//            }
//            flushCaches();
//        } catch (Exception e) {
//            super.addNonTranslatedErrorMessage(e.getMessage());
//        }
    }

    /** Invoked when admin requests the removal of multiple key pair. */
    public void removeSelectedKeyPairs() {
//        if (keyPairGuiInfos!=null) {
//            for (KeyPairGuiInfo cryptoTokenKeyPairInfo : keyPairGuiInfos) {
//                if (cryptoTokenKeyPairInfo.isSelected()) {
//                    try {
//                        vpnUserManagementSession.removeKeyPair(getAdmin(), getCurrentVpnUserId(), cryptoTokenKeyPairInfo.getAlias());
//                    } catch (Exception e) {
//                        super.addNonTranslatedErrorMessage(e.getMessage());
//                    }
//                }
//            }
//        }
//        flushCaches();
    }
}
