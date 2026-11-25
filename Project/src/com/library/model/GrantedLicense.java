package com.library.model;

/**
 * Entity representing an access license granted to an individual (GRANTED_LICENSE).
 * Đã loại bỏ valid_from và valid_to: chỉ xác nhận sự tồn tại của bản ghi cấp quyền.
 */
public class GrantedLicense {
    private int grantId;
    private int personId;
    private int licenseId;

    public GrantedLicense() {
    }

    public GrantedLicense(int grantId, int personId, int licenseId) {
        this.grantId = grantId;
        this.personId = personId;
        this.licenseId = licenseId;
    }

    public int getGrantId() {
        return grantId;
    }

    public void setGrantId(int grantId) {
        this.grantId = grantId;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public int getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(int licenseId) {
        this.licenseId = licenseId;
    }

    @Override
    public String toString() {
        return "GrantedLicense{" +
                "grantId=" + grantId +
                ", personId=" + personId +
                ", licenseId=" + licenseId +
                '}';
    }
}
