package com.library.model;

/**
 * Entity representing a license package providing access rights to specific digital resources.
 */
public class LicensePackage {
    private int licenseId;
    private String licenseName;
    private String provider;
    private String description;

    public LicensePackage() {
    }

    public LicensePackage(int licenseId, String licenseName, String provider, String description) {
        this.licenseId = licenseId;
        this.licenseName = licenseName;
        this.provider = provider;
        this.description = description;
    }

    public int getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(int licenseId) {
        this.licenseId = licenseId;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "LicensePackage{" +
                "licenseId=" + licenseId +
                ", licenseName='" + licenseName + '\'' +
                ", provider='" + provider + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
