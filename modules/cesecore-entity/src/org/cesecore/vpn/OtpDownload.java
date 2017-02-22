package org.cesecore.vpn;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.security.KeyStore;

/**
 * One Time Download tokens
 *
 * @author ph4r05
 * Created by dusanklinec on 23.12.16.
 */
@Entity
@Table(name = "OtpDownload")
public class OtpDownload implements Serializable, Cloneable {
    private static final long serialVersionUID = 3304969235926944711L;

    private int rowVersion = 0;
    private String rowProtection;

    @Id
    private Integer id;

    /**
     * OTP type identifier, e.g., p12, ovpn, ...
     */
    private String otpType;

    /**
     * OTP resource identification, e.g., user/device
     */
    private String otpId;

    /**
     * OTP resource this grants permission to.
     */
    private String otpResource;

    private long dateCreated;
    private long dateModified;

    /**
     * One time download token.
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
     * Serialized aux data
     */
    private String auxData;

    public OtpDownload() {
    }

    public OtpDownload(String otpType, String otpId) {
        this.otpType = otpType;
        this.otpId = otpId;
    }

    public OtpDownload(int rowVersion, String rowProtection, Integer id, String otpType, String otpId, String otpResource, long dateCreated, long dateModified, String otpDownload, Long otpFirstUsed, Long otpUsed, String otpUsedDescriptor, String otpCookie, int otpUsedCount, String auxData) {
        this.rowVersion = rowVersion;
        this.rowProtection = rowProtection;
        this.id = id;
        this.otpType = otpType;
        this.otpId = otpId;
        this.otpResource = otpResource;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
        this.otpDownload = otpDownload;
        this.otpFirstUsed = otpFirstUsed;
        this.otpUsed = otpUsed;
        this.otpUsedDescriptor = otpUsedDescriptor;
        this.otpCookie = otpCookie;
        this.otpUsedCount = otpUsedCount;
        this.auxData = auxData;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Makes a shallow copy of the source vpn user.
     * @param src
     * @return OtpDownload, not connected to the persistence context.
     */
    public static OtpDownload copy(OtpDownload src) {
        try {
            final OtpDownload obj = (OtpDownload) src.clone();
            return obj;
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

    public String getOtpType() {
        return otpType;
    }

    public void setOtpType(String otpType) {
        this.otpType = otpType;
    }

    public String getOtpId() {
        return otpId;
    }

    public void setOtpId(String otpId) {
        this.otpId = otpId;
    }

    public String getOtpResource() {
        return otpResource;
    }

    public void setOtpResource(String otpResource) {
        this.otpResource = otpResource;
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

    public String getOtpDownload() {
        return otpDownload;
    }

    public void setOtpDownload(String otpDownload) {
        this.otpDownload = otpDownload;
    }

    public Long getOtpFirstUsed() {
        return otpFirstUsed;
    }

    public void setOtpFirstUsed(Long otpFirstUsed) {
        this.otpFirstUsed = otpFirstUsed;
    }

    public Long getOtpUsed() {
        return otpUsed;
    }

    public void setOtpUsed(Long otpUsed) {
        this.otpUsed = otpUsed;
    }

    public String getOtpUsedDescriptor() {
        return otpUsedDescriptor;
    }

    public void setOtpUsedDescriptor(String otpUsedDescriptor) {
        this.otpUsedDescriptor = otpUsedDescriptor;
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

    public String getAuxData() {
        return auxData;
    }

    public void setAuxData(String auxData) {
        this.auxData = auxData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OtpDownload that = (OtpDownload) o;

        if (rowVersion != that.rowVersion) return false;
        if (dateCreated != that.dateCreated) return false;
        if (dateModified != that.dateModified) return false;
        if (otpUsedCount != that.otpUsedCount) return false;
        if (rowProtection != null ? !rowProtection.equals(that.rowProtection) : that.rowProtection != null)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (otpType != null ? !otpType.equals(that.otpType) : that.otpType != null) return false;
        if (otpId != null ? !otpId.equals(that.otpId) : that.otpId != null) return false;
        if (otpResource != null ? !otpResource.equals(that.otpResource) : that.otpResource != null) return false;
        if (otpDownload != null ? !otpDownload.equals(that.otpDownload) : that.otpDownload != null) return false;
        if (otpFirstUsed != null ? !otpFirstUsed.equals(that.otpFirstUsed) : that.otpFirstUsed != null) return false;
        if (otpUsed != null ? !otpUsed.equals(that.otpUsed) : that.otpUsed != null) return false;
        if (otpUsedDescriptor != null ? !otpUsedDescriptor.equals(that.otpUsedDescriptor) : that.otpUsedDescriptor != null)
            return false;
        return otpCookie != null ? otpCookie.equals(that.otpCookie) : that.otpCookie == null;
    }

    @Override
    public int hashCode() {
        int result = rowVersion;
        result = 31 * result + (rowProtection != null ? rowProtection.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (otpType != null ? otpType.hashCode() : 0);
        result = 31 * result + (otpId != null ? otpId.hashCode() : 0);
        result = 31 * result + (otpResource != null ? otpResource.hashCode() : 0);
        result = 31 * result + (int) (dateCreated ^ (dateCreated >>> 32));
        result = 31 * result + (int) (dateModified ^ (dateModified >>> 32));
        result = 31 * result + (otpDownload != null ? otpDownload.hashCode() : 0);
        result = 31 * result + (otpFirstUsed != null ? otpFirstUsed.hashCode() : 0);
        result = 31 * result + (otpUsed != null ? otpUsed.hashCode() : 0);
        result = 31 * result + (otpUsedDescriptor != null ? otpUsedDescriptor.hashCode() : 0);
        result = 31 * result + (otpCookie != null ? otpCookie.hashCode() : 0);
        result = 31 * result + otpUsedCount;
        return result;
    }
}
