package com.library.model;

import java.time.LocalDate;

/**
 * Entity representing an access license granted to an individual.
 */
public class GrantedLicense {
    private int grantId;
    private int personId;
    private int licenseId;
    private LocalDate validFrom;
    private LocalDate validTo;

    public GrantedLicense() {
    }

    public GrantedLicense(int grantId, int personId, int licenseId, LocalDate validFrom, LocalDate validTo) {
        this.grantId = grantId;
        this.personId = personId;
        this.licenseId = licenseId;
        this.validFrom = validFrom;
        this.validTo = validTo;
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

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    @Override
    public String toString() {
        return "GrantedLicense{" +
                "grantId=" + grantId +
                ", personId=" + personId +
                ", licenseId=" + licenseId +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                '}';
    }
}
