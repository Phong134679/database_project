package com.library.model;

/**
 * Associative entity representing the many-to-many relationship
 * between Person and Resource (AUTHOR_OF).
 */
public class AuthorOf {
    private int personId;
    private int resourceId;

    public AuthorOf() {
    }

    public AuthorOf(int personId, int resourceId) {
        this.personId = personId;
        this.resourceId = resourceId;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return "AuthorOf{" +
                "personId=" + personId +
                ", resourceId=" + resourceId +
                '}';
    }
}
