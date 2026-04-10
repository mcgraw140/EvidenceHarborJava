package com.evidenceharbor.domain;

public class Charge {
    private int id;
    private String code;
    private String description;

    public Charge() {}
    public Charge(int id, String code, String description) { this.id=id; this.code=code; this.description=description; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override public String toString() { return code + " - " + description; }
}
