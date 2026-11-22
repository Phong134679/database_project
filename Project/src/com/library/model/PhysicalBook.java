package com.library.model;

import java.time.LocalDate;

/**
 * Subclass of Resource representing a physical book title.
 * ISBN attribute has been removed per library schema simplification.
 */
public class PhysicalBook extends Resource {
    private String shelfLocation;

    public PhysicalBook() {
        super();
    }

    public PhysicalBook(int resourceId, String title, LocalDate publishDate, String topic, String resourceType,
                        String shelfLocation) {
        super(resourceId, title, publishDate, topic, resourceType);
        this.shelfLocation = shelfLocation;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    @Override
    public String toString() {
        return "PhysicalBook{" +
                "resourceId=" + getResourceId() +
                ", title='" + getTitle() + '\'' +
                ", shelfLocation='" + shelfLocation + '\'' +
                '}';
    }
}
