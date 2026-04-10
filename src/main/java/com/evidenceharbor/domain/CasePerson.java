package com.evidenceharbor.domain;

public class CasePerson {
    private int id;
    private int caseId;
    private Person person;
    private String role;

    public CasePerson() {}
    public CasePerson(int id, int caseId, Person person, String role) {
        this.id=id; this.caseId=caseId; this.person=person; this.role=role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCaseId() { return caseId; }
    public void setCaseId(int caseId) { this.caseId = caseId; }
    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
