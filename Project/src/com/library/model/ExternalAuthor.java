package com.library.model;

/**
 * Subclass of Person representing external authors contributing materials to the library.
 */
public class ExternalAuthor extends Person {
    private String affiliation;
    private String biography;

    public ExternalAuthor() {
        super();
    }

    public ExternalAuthor(int personId, String ssn, String name, String address, String email, String phone,
                          String personType, String affiliation, String biography) {
        super(personId, ssn, name, address, email, phone, personType);
        this.affiliation = affiliation;
        this.biography = biography;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    @Override
    public String toString() {
        return "ExternalAuthor{" +
                "personId=" + getPersonId() +
                ", ssn='" + getSsn() + '\'' +
                ", name='" + getName() + '\'' +
                ", affiliation='" + affiliation + '\'' +
                ", biography='" + biography + '\'' +
                '}';
    }
}
