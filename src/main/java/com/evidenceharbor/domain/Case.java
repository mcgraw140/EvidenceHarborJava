package com.evidenceharbor.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Case {
    private int id;
    private String caseNumber;
    private LocalDate incidentDate;
    private Officer officer;
    private List<CasePerson> persons = new ArrayList<>();
    private List<Charge> charges = new ArrayList<>();

    public Case() {}
    public Case(int id, String caseNumber, LocalDate incidentDate, Officer officer) {
        this.id=id; this.caseNumber=caseNumber; this.incidentDate=incidentDate; this.officer=officer;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDate incidentDate) { this.incidentDate = incidentDate; }
    public Officer getOfficer() { return officer; }
    public void setOfficer(Officer officer) { this.officer = officer; }
    public List<CasePerson> getPersons() { return persons; }
    public void setPersons(List<CasePerson> persons) { this.persons = persons; }
    public List<Charge> getCharges() { return charges; }
    public void setCharges(List<Charge> charges) { this.charges = charges; }
}
