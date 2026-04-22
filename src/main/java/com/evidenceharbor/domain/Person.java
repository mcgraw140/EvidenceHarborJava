package com.evidenceharbor.domain;

public class Person {
    private int id;
    private String fullName;
    private String dob;
    private String ssn;
    private String street;
    private String city;
    private String state;
    private String zip;
    private String contact;

    public Person() {}
    public Person(int id, String fullName) { this.id=id; this.fullName=fullName; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    @Override public String toString() { return fullName; }
}
