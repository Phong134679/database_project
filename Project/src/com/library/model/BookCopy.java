package com.library.model;

/**
 * Entity representing a specific physical copy of a book title.
 * copyId is formatted as '<resource_id>_<index>' (String).
 * Barcode and conditionStatus have been eliminated.
 */
public class BookCopy {
    private String copyId;
    private int resourceId;
    private String availabilityStatus;

    public BookCopy() {
    }

    public BookCopy(String copyId, int resourceId, String availabilityStatus) {
        this.copyId = copyId;
        this.resourceId = resourceId;
        this.availabilityStatus = availabilityStatus;
    }

    public String getCopyId() {
        return copyId;
    }

    public void setCopyId(String copyId) {
        this.copyId = copyId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
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
                "copyId='" + copyId + '\'' +
                ", resourceId=" + resourceId +
                ", availabilityStatus='" + availabilityStatus + '\'' +
                '}';
    }
}
