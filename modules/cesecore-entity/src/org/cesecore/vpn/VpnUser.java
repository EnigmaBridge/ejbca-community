package org.cesecore.vpn;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.security.KeyStore;

/**
 * VpnUser DB entity.
 * Stores basic VPN user data, related to EJBCA VPN module.
 * One record represents user + device.
 *
 * @author ph4r05
 * Created by dusanklinec on 23.12.16.
 */
@Entity
@Table(name = "VpnUser")
public class VpnUser implements Serializable, Cloneable {
    private static final long serialVersionUID = 3304969235926944711L;

    private int rowVersion = 0;
    private String rowProtection;

    @Id
    private Integer id;

    /**
     * Email + device is unique identifier of the VPN user.
     */
    private String email;
    private String device;

    /**
     * Primary language user communicates.
     * If possible, emails are localised using this language
     */
    private String usrLang;

    private long dateCreated;
    private long dateModified;
    private int revokedStatus;

    /**
     * One time download token used to download OVPN configuration.
     */
    private String otpDownload;

    /**
     * UTCmilli encoded time of first OTP download of the same token. Null if OTP has not been used yet.
     */
    private Long otpFirstUsed;

    /**
     * UTCmilli encoded time of last OTP download. Null if OTP has not been used yet.
     */
    private Long otpUsed;

    /**
     * Json encoded descriptor of the agent which downloaded the configuration with OTP (e.g., IP, useragent)
     */
    private String otpUsedDescriptor;

    /**
     * OTP cookie set on the client, to allow more requests.
     */
    private String otpCookie;

    /**
     * Number of times the OTP token was authorised and content was provided.
     */
    private int otpUsedCount;

    /**
     * Last configuration email send to the user.
     * If null, no configuration email for the current key/config has been sent.
     */
    private Long lastMailSent;
    private String certificateId;
    private String certificate;

    /**
     * Base64 encoded keystore contains user certificate.
     */
    private String keyStore;
    private String vpnConfig;

    /**
     * Timestamp when the config was generated
     */
    private Long configGenerated;

    /**
     * VPN config version sequence number.
     */
    private Integer configVersion=1;

    /**
     * Associated administrator user name role.
     */
    private String adminRole;

    /**
     * Raw key store object for transfer from the create routines.
     * Not stored nor serialized.
     */
    @Transient
    private KeyStore keyStoreRaw;

    public VpnUser() {
    }

    public VpnUser(String email, String device) {
        this.email = email;
        this.device = device;
    }

    public VpnUser(int rowVersion, String rowProtection, Integer id, String email, String device, String usrLang, long dateCreated, long dateModified, int revokedStatus, String otpDownload, Long otpFirstUsed, Long otpUsed, String otpUsedDescriptor, String otpCookie, int otpUsedCount, Long lastMailSent, String certificateId, String certificate, String keyStore, String vpnConfig, Long configGenerated, Integer configVersion, String adminRole) {
        this.rowVersion = rowVersion;
        this.rowProtection = rowProtection;
        this.id = id;
        this.email = email;
        this.device = device;
        this.usrLang = usrLang;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
        this.revokedStatus = revokedStatus;
        this.otpDownload = otpDownload;
        this.otpFirstUsed = otpFirstUsed;
        this.otpUsed = otpUsed;
        this.otpUsedDescriptor = otpUsedDescriptor;
        this.otpCookie = otpCookie;
        this.otpUsedCount = otpUsedCount;
        this.lastMailSent = lastMailSent;
        this.certificateId = certificateId;
        this.certificate = certificate;
        this.keyStore = keyStore;
        this.vpnConfig = vpnConfig;
        this.configGenerated = configGenerated;
        this.configVersion = configVersion;
        this.adminRole = adminRole;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Makes a shallow copy of the source vpn user.
     * @param src
     * @return VpnUser, not connected to the persistence context.
     */
    public static VpnUser copy(VpnUser src) {
        try {
            final VpnUser vpnUser = (VpnUser) src.clone();
            return vpnUser;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unexpected clone exception", e);
        }
    }

    public int getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(int rowVersion) {
        this.rowVersion = rowVersion;
    }

    public String getRowProtection() {
        return rowProtection;
    }

    public void setRowProtection(String rowProtection) {
        this.rowProtection = rowProtection;
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
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(long date_created) {
        this.dateCreated = date_created;
    }

    public long getDateModified() {
        return dateModified;
    }

    public void setDateModified(long date_modified) {
        this.dateModified = date_modified;
    }

    public int getRevokedStatus() {
        return revokedStatus;
    }

    public void setRevokedStatus(int revoked_status) {
        this.revokedStatus = revoked_status;
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

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String key) {
        this.keyStore = key;
    }

    public String getVpnConfig() {
        return vpnConfig;
    }

    public void setVpnConfig(String config) {
        this.vpnConfig = config;
    }

    public Long getOtpUsed() {
        return otpUsed;
    }

    public void setOtpUsed(Long otpUsed) {
        this.otpUsed = otpUsed;
    }

    public Long getLastMailSent() {
        return lastMailSent;
    }

    public void setLastMailSent(Long lastMailSent) {
        this.lastMailSent = lastMailSent;
    }

    public String getUsrLang() {
        return usrLang;
    }

    public void setUsrLang(String language) {
        this.usrLang = language;
    }

    public Integer getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(Integer configVersion) {
        this.configVersion = configVersion;
    }

    public Long getConfigGenerated() {
        return configGenerated;
    }

    public void setConfigGenerated(Long configGenerated) {
        this.configGenerated = configGenerated;
    }

    public String getOtpUsedDescriptor() {
        return otpUsedDescriptor;
    }

    public void setOtpUsedDescriptor(String otpUsedDescriptor) {
        this.otpUsedDescriptor = otpUsedDescriptor;
    }

    public Long getOtpFirstUsed() {
        return otpFirstUsed;
    }

    public void setOtpFirstUsed(Long otpFirstUsed) {
        this.otpFirstUsed = otpFirstUsed;
    }

    public String getOtpCookie() {
        return otpCookie;
    }

    public void setOtpCookie(String otpCookie) {
        this.otpCookie = otpCookie;
    }

    public int getOtpUsedCount() {
        return otpUsedCount;
    }

    public void setOtpUsedCount(int otpUsedCount) {
        this.otpUsedCount = otpUsedCount;
    }

    public String getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }

    @Transient
    public KeyStore getKeyStoreRaw() {
        return keyStoreRaw;
    }

    @Transient
    public void setKeyStoreRaw(KeyStore keyStoreRaw) {
        this.keyStoreRaw = keyStoreRaw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VpnUser vpnUser = (VpnUser) o;

        if (rowVersion != vpnUser.rowVersion) return false;
        if (dateCreated != vpnUser.dateCreated) return false;
        if (dateModified != vpnUser.dateModified) return false;
        if (revokedStatus != vpnUser.revokedStatus) return false;
        if (otpUsedCount != vpnUser.otpUsedCount) return false;
        if (rowProtection != null ? !rowProtection.equals(vpnUser.rowProtection) : vpnUser.rowProtection != null)
            return false;
        if (id != null ? !id.equals(vpnUser.id) : vpnUser.id != null) return false;
        if (email != null ? !email.equals(vpnUser.email) : vpnUser.email != null) return false;
        if (device != null ? !device.equals(vpnUser.device) : vpnUser.device != null) return false;
        if (usrLang != null ? !usrLang.equals(vpnUser.usrLang) : vpnUser.usrLang != null) return false;
        if (otpDownload != null ? !otpDownload.equals(vpnUser.otpDownload) : vpnUser.otpDownload != null) return false;
        if (otpFirstUsed != null ? !otpFirstUsed.equals(vpnUser.otpFirstUsed) : vpnUser.otpFirstUsed != null)
            return false;
        if (otpUsed != null ? !otpUsed.equals(vpnUser.otpUsed) : vpnUser.otpUsed != null) return false;
        if (otpUsedDescriptor != null ? !otpUsedDescriptor.equals(vpnUser.otpUsedDescriptor) : vpnUser.otpUsedDescriptor != null)
            return false;
        if (otpCookie != null ? !otpCookie.equals(vpnUser.otpCookie) : vpnUser.otpCookie != null) return false;
        if (lastMailSent != null ? !lastMailSent.equals(vpnUser.lastMailSent) : vpnUser.lastMailSent != null)
            return false;
        if (certificateId != null ? !certificateId.equals(vpnUser.certificateId) : vpnUser.certificateId != null)
            return false;
        if (certificate != null ? !certificate.equals(vpnUser.certificate) : vpnUser.certificate != null) return false;
        if (keyStore != null ? !keyStore.equals(vpnUser.keyStore) : vpnUser.keyStore != null) return false;
        if (vpnConfig != null ? !vpnConfig.equals(vpnUser.vpnConfig) : vpnUser.vpnConfig != null) return false;
        if (configGenerated != null ? !configGenerated.equals(vpnUser.configGenerated) : vpnUser.configGenerated != null)
            return false;
        if (configVersion != null ? !configVersion.equals(vpnUser.configVersion) : vpnUser.configVersion != null)
            return false;
        return adminRole != null ? adminRole.equals(vpnUser.adminRole) : vpnUser.adminRole == null;
    }

    @Override
    public int hashCode() {
        int result = rowVersion;
        result = 31 * result + (rowProtection != null ? rowProtection.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (device != null ? device.hashCode() : 0);
        result = 31 * result + (usrLang != null ? usrLang.hashCode() : 0);
        result = 31 * result + (int) (dateCreated ^ (dateCreated >>> 32));
        result = 31 * result + (int) (dateModified ^ (dateModified >>> 32));
        result = 31 * result + revokedStatus;
        result = 31 * result + (otpDownload != null ? otpDownload.hashCode() : 0);
        result = 31 * result + (otpFirstUsed != null ? otpFirstUsed.hashCode() : 0);
        result = 31 * result + (otpUsed != null ? otpUsed.hashCode() : 0);
        result = 31 * result + (otpUsedDescriptor != null ? otpUsedDescriptor.hashCode() : 0);
        result = 31 * result + (otpCookie != null ? otpCookie.hashCode() : 0);
        result = 31 * result + otpUsedCount;
        result = 31 * result + (lastMailSent != null ? lastMailSent.hashCode() : 0);
        result = 31 * result + (certificateId != null ? certificateId.hashCode() : 0);
        result = 31 * result + (certificate != null ? certificate.hashCode() : 0);
        result = 31 * result + (keyStore != null ? keyStore.hashCode() : 0);
        result = 31 * result + (vpnConfig != null ? vpnConfig.hashCode() : 0);
        result = 31 * result + (configGenerated != null ? configGenerated.hashCode() : 0);
        result = 31 * result + (configVersion != null ? configVersion.hashCode() : 0);
        result = 31 * result + (adminRole != null ? adminRole.hashCode() : 0);
        return result;
    }
}
