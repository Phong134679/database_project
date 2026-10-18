package com.library.model;

import java.time.LocalDate;

/**
 * Subclass of Resource representing a physical book title.
 */
public class PhysicalBook extends Resource {
    private String isbn;
    private String shelfLocation;

    public PhysicalBook() {
        super();
    }

    public PhysicalBook(int resourceId, String title, LocalDate publishDate, String topic, String resourceType,
                        String isbn, String shelfLocation) {
        super(resourceId, title, publishDate, topic, resourceType);
        this.isbn = isbn;
        this.shelfLocation = shelfLocation;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
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
                ", isbn='" + isbn + '\'' +
                ", shelfLocation='" + shelfLocation + '\'' +
                '}';
    }
}
