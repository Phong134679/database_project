package com.library.model;

/**
 * Entity representing a specific physical copy of a book title.
 */
public class BookCopy {
    private int copyId;
    private int resourceId;
    private String barcode;
    private String conditionStatus;
    private String availabilityStatus;

    public BookCopy() {
    }

    public BookCopy(int copyId, int resourceId, String barcode, String conditionStatus, String availabilityStatus) {
        this.copyId = copyId;
        this.resourceId = resourceId;
        this.barcode = barcode;
        this.conditionStatus = conditionStatus;
        this.availabilityStatus = availabilityStatus;
    }

    public int getCopyId() {
        return copyId;
    }

    public void setCopyId(int copyId) {
        this.copyId = copyId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getConditionStatus() {
        return conditionStatus;
    }

    public void setConditionStatus(String conditionStatus) {
        this.conditionStatus = conditionStatus;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    @Override
    public String toString() {
        return "BookCopy{" +
                "copyId=" + copyId +
                ", resourceId=" + resourceId +
                ", barcode='" + barcode + '\'' +
                ", conditionStatus='" + conditionStatus + '\'' +
                ", availabilityStatus='" + availabilityStatus + '\'' +
                '}';
    }
}
