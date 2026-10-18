package com.library.model;

/**
 * Associative entity representing the many-to-many relationship
 * between Resource and LicensePackage (RESOURCE_LICENSE).
 */
public class ResourceLicense {
    private int resourceId;
    private int licenseId;

    public ResourceLicense() {
    }

    public ResourceLicense(int resourceId, int licenseId) {
        this.resourceId = resourceId;
        this.licenseId = licenseId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(int licenseId) {
        this.licenseId = licenseId;
    }

    @Override
    public String toString() {
        return "ResourceLicense{" +
                "resourceId=" + resourceId +
                ", licenseId=" + licenseId +
                '}';
    }
}
