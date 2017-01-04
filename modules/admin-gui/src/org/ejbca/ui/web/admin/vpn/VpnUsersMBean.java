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
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.certificates.util.AlgorithmTools;
import org.cesecore.keybind.InternalKeyBindingMgmtSessionLocal;
import org.cesecore.vpn.VpnUserManagementSession;
import org.cesecore.vpn.VpnUser;
import org.ejbca.ui.web.admin.BaseManagedBean;

import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.*;

/**
 * JavaServer Faces Managed Bean for managing VPN users.
 * Session scoped and will cache the list of tokens and keys.
 * 
 * @version $Id: VpnUsersMBean.java 20320 2014-11-25 18:05:45Z mikekushner $
 */
public class VpnUsersMBean extends BaseManagedBean implements Serializable {

    private static final String CRYPTOTOKEN_LABEL_TYPE_TEXTPREFIX = "CRYPTOTOKEN_LABEL_TYPE_";
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(VpnUsersMBean.class);

    /** GUI table representation of a VPN user that can be interacted with. */
    public class VpnUserGuiInfo {
        private Integer id;
        private String user;
        private Date dateCreated;
        private Date dateModified;
        private boolean revoked;
        private Certificate certificate;
        private PrivateKey key;
        private boolean selected = false;

        public VpnUserGuiInfo() {
        }

        public VpnUserGuiInfo(Integer id, String user, Date dateCreated, Date dateModified, boolean revoked, Certificate certificate) {
            this.id = id;
            this.user = user;
            this.dateCreated = dateCreated;
            this.dateModified = dateModified;
            this.revoked = revoked;
            this.certificate = certificate;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
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
    }

    /** GUI edit/view representation of a VpnUser that can be interacted with. */
    public class CurrentVpnUserGuiInfo {
        private String name = "";
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

        private CurrentVpnUserGuiInfo() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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
    }

    private List<VpnUserGuiInfo> vpnUserGuiInfos = new ArrayList<VpnUserGuiInfo>();

    @SuppressWarnings("rawtypes") //JDK6 does not support typing for ListDataModel
    private ListDataModel vpnUserGuiList = null;

    @SuppressWarnings("rawtypes") //JDK6 does not support typing for ListDataModel
    private ListDataModel keyPairGuiList = null;
    private String keyPairGuiListError = null;
    private String currenVpnUserId = null;
    private CurrentVpnUserGuiInfo currentVpnUser = null;
    private boolean currentCryptoTokenEditMode = true;  // currenVpnUserId==0 from start

    private final VpnUserManagementSession vpnUserManagementSession = getEjbcaWebBean().getEjb().getVpnUserManagementSession();

    private final AccessControlSessionLocal accessControlSession = getEjbcaWebBean().getEjb().getAccessControlSession();
    private final AuthenticationToken authenticationToken = getAdmin();
    private final CaSessionLocal caSession = getEjbcaWebBean().getEjb().getCaSession();
    private final InternalKeyBindingMgmtSessionLocal internalKeyBindingMgmtSession = getEjbcaWebBean().getEjb().getInternalKeyBindingMgmtSession();

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

    /** Build a list sorted by name from the authorized cryptoTokens that can be presented to the user */
    @SuppressWarnings({ "rawtypes", "unchecked" }) //JDK6 does not support typing for ListDataModel
    public ListDataModel getVpnUserGuiList() throws AuthorizationDeniedException {
        if (vpnUserGuiList ==null) {
            final List<String> vpnUserIds = vpnUserManagementSession.geVpnUsersIds(authenticationToken);
            final List<VpnUserGuiInfo> users = new ArrayList<>(vpnUserIds.size());
            for(String userId : vpnUserIds){
                final VpnUser vpnUser = vpnUserManagementSession.getVpnUser(authenticationToken, userId);
                users.add(toGuiUser(vpnUser));
            }

            Collections.sort(users, new Comparator<VpnUserGuiInfo>() {
                @Override
                public int compare(VpnUserGuiInfo a, VpnUserGuiInfo b) {
                    return a.getUser().compareTo(b.getUser());
                }
            });

            vpnUserGuiInfos = users;
            vpnUserGuiList = new ListDataModel(vpnUserGuiInfos);
        }
        // If show the list, then we are on the main page and want to flush the two caches
        flushCurrent();
        setCurrentCryptoTokenEditMode(false);
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
            final VpnUserGuiInfo rowData = (VpnUserGuiInfo) vpnUserGuiList.getRowData();
            vpnUserManagementSession.deleteVpnUser(authenticationToken, rowData.getUser());
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

        user.setUsername(guiUser.getName());
        user.setDateModified(guiUser.getDateModified());
        user.setRevokedStatus(guiUser.getRevokedStatus());
        user.setOtpDownload(guiUser.getOtpDownload());
        user.setCertificateId(guiUser.getCertificateId());
        user.setCertificate(guiUser.getCertificate());
        user.setKey(guiUser.getKey());
        user.setConfig(guiUser.getConfig());

        return user;
    }

    private VpnUserGuiInfo toGuiUser(VpnUser vpnUser){
        final VpnUserGuiInfo user = new VpnUserGuiInfo();

        user.setUser(vpnUser.getUsername());
        user.setDateCreated(new Date(vpnUser.getDateCreated()));
        user.setDateModified(new Date(vpnUser.getDateModified()));
        user.setRevoked(vpnUser.getRevokedStatus() > 0);

        return user;
    }

    /** Invoked when admin requests a CryptoToken creation. */
    public void saveCurrentCryptoToken() throws AuthorizationDeniedException {
        String msg = null;
        try {
            final Properties properties = new Properties();
            final String name = getCurrentVpnUser().getName();
            if (getCurrentVpnUserId() == null) {

                final VpnUser newVpnUser = vpnUserManagementSession.createVpnUser(authenticationToken,
                        fromGuiUser(getCurrentVpnUser()));

                currenVpnUserId = newVpnUser.getUsername();
                msg = "VpnUser created successfully.";

            } else {
                vpnUserManagementSession.saveVpnUser(authenticationToken, fromGuiUser(getCurrentVpnUser()));
                msg = "VpnUser saved successfully.";
            }

            flushCaches();
            setCurrentCryptoTokenEditMode(false);

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

    /** Invoked when admin cancels a CryptoToken create or edit. */
    public void cancelCurrentCryptoToken() {
        setCurrentCryptoTokenEditMode(false);
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
    
    /** @return the id of the CryptoToken that is subject to view or edit */
    public String getCurrentVpnUserId() {
        // Get the HTTP GET/POST parameter named "cryptoTokenId"
        String vpnUserIdString = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("vpnUserId");
        if (vpnUserIdString!=null) {
            if (vpnUserIdString.isEmpty()){
                vpnUserIdString = null;
            }

            // If there is a query parameter present and the id is different we flush the cache!
            if (vpnUserIdString != this.currenVpnUserId) {
                flushCaches();
                this.currenVpnUserId = vpnUserIdString;
            }
            // Always switch to edit mode for new ones and view mode for all others
            setCurrentCryptoTokenEditMode(vpnUserIdString == null);
        }
        return currenVpnUserId;
    }

    /** @return cached or populate a new CryptoToken GUI representation for view or edit */
    public CurrentVpnUserGuiInfo getCurrentVpnUser() throws AuthorizationDeniedException {
        if (this.currentVpnUser == null) {
            final String vpnUserId = getCurrentVpnUserId();
            final CurrentVpnUserGuiInfo currentVpnUser = new CurrentVpnUserGuiInfo();
            // If the id is non-zero we try to load an existing token
            if (vpnUserId!=null) {
                final VpnUser vpnUser = vpnUserManagementSession.getVpnUser(authenticationToken, vpnUserId);
                if (vpnUser == null) {
                    throw new RuntimeException("Could not load VpnUser with vpnUserId " + vpnUserId);
                } else {
                    currentVpnUser.setName(vpnUser.getUsername());
                    currentVpnUser.setDateCreated(vpnUser.getDateCreated());
                    currentVpnUser.setDateModified(vpnUser.getDateModified());
                    currentVpnUser.setRevokedStatus(vpnUser.getRevokedStatus());
                    currentVpnUser.setOtpDownload(vpnUser.getOtpDownload());
                    currentVpnUser.setCertificateId(vpnUser.getCertificateId());
                    currentVpnUser.setCertificate(vpnUser.getCertificate());
                    currentVpnUser.setKey(vpnUser.getKey());
                    currentVpnUser.setConfig(vpnUser.getConfig());
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
    public void selectCryptoTokenLabelType() {
        // Clear slot reference when we change type
//        currentVpnUser.setP11Slot("");
    }

    public boolean isCurrentCryptoTokenEditMode() {
        return currentCryptoTokenEditMode;
    }

    public void setCurrentCryptoTokenEditMode(boolean currentCryptoTokenEditMode) {
        this.currentCryptoTokenEditMode = currentCryptoTokenEditMode;
    }

    public void toggleCurrentCryptoTokenEditMode() {
        currentCryptoTokenEditMode ^= true;
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
