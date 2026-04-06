package com.evidenceharbor.model;

import java.time.LocalDate;

public class Case {
    private String caseNumber;
    private String description;
    private String officer;
    private LocalDate date;
    private String location;
    private String notes;

    // Constructors, getters, setters
    public Case() {}

    public Case(String caseNumber, String description, String officer, LocalDate date, String location, String notes) {
        this.caseNumber = caseNumber;
        this.description = description;
        this.officer = officer;
        this.date = date;
        this.location = location;
        this.notes = notes;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOfficer() {
        return officer;
    }

    public void setOfficer(String officer) {
        this.officer = officer;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}