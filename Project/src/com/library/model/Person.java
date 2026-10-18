package com.library.model;

/**
 * Superclass representing an individual profile in the system.
 */
public class Person {
    private int personId;
    private String ssn;
    private String name;
    private String address;
    private String email;
    private String phone;
    private String personType;

    public Person() {
    }

    public Person(int personId, String ssn, String name, String address, String email, String phone, String personType) {
        this.personId = personId;
        this.ssn = ssn;
        this.name = name;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.personType = personType;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPersonType() {
        return personType;
    }

    public void setPersonType(String personType) {
        this.personType = personType;
    }

    @Override
    public String toString() {
        return "Person{" +
                "personId=" + personId +
                ", ssn='" + ssn + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", personType='" + personType + '\'' +
                '}';
    }
}
