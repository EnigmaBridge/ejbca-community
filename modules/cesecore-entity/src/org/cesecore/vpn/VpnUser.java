package org.cesecore.vpn;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Created by dusanklinec on 23.12.16.
 */
@Entity
@Table(name = "VpnUser")
public class VpnUser implements Serializable {
    private static final long serialVersionUID = 3304969235926944711L;

    private int rowVersion = 0;
    private String rowProtection;

    @Id
    private Integer id;

    private String email;
    private String device;

    private long dateCreated;
    private long dateModified;
    private int revokedStatus;
    private String otpDownload;
    private String certificateId;
    private String certificate;
    private String key;
    private String config;

    public VpnUser() {
    }

    public VpnUser(String email) {
        this.email = email;
    }

    public VpnUser(String email, String device, long dateCreated, long dateModified, int revokedStatus, String otpDownload, String certificateId, String certificate, String key, String config) {
        this.email = email;
        this.device = device;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
        this.revokedStatus = revokedStatus;
        this.otpDownload = otpDownload;
        this.certificateId = certificateId;
        this.certificate = certificate;
        this.key = key;
        this.config = config;
    }

    public VpnUser(int rowVersion, String rowProtection, Integer id, String email, String device, long dateCreated, long dateModified, int revokedStatus, String otpDownload, String certificateId, String certificate, String key, String config) {
        this.rowVersion = rowVersion;
        this.rowProtection = rowProtection;
        this.id = id;
        this.email = email;
        this.device = device;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
        this.revokedStatus = revokedStatus;
        this.otpDownload = otpDownload;
        this.certificateId = certificateId;
        this.certificate = certificate;
        this.key = key;
        this.config = config;
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
