package org.cesecore.vpn;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

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
     * UTCmilli encoded time of last OTP download. Null if OTP has not been used yet.
     */
    private Long otpUsed;

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
     * VPN config version sequence number.
     */
    private Integer configVersion=1;

    public VpnUser() {
    }

    public VpnUser(String email, String device) {
        this.email = email;
        this.device = device;
    }

    public VpnUser(int rowVersion, String rowProtection, Integer id, String email, String device, String usrLang, long dateCreated, long dateModified, int revokedStatus, String otpDownload, Long otpUsed, Long lastMailSent, String certificateId, String certificate, String keyStore, String vpnConfig, int configVersion) {
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
        this.otpUsed = otpUsed;
        this.lastMailSent = lastMailSent;
        this.certificateId = certificateId;
        this.certificate = certificate;
        this.keyStore = keyStore;
        this.vpnConfig = vpnConfig;
        this.configVersion = configVersion;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Makes a shallow copy of the source vpn user.
     * @param src
     * @return
     * @throws CloneNotSupportedException
     */
    public static VpnUser copy(VpnUser src) throws CloneNotSupportedException {
        final VpnUser vpnUser = (VpnUser) src.clone();
        return vpnUser;
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
}
