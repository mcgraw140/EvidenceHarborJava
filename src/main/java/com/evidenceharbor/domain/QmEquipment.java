package com.evidenceharbor.domain;

public class QmEquipment {
    private int id;
    private String name;
    private String category;
    private String make;
    private String model;
    private String serial;
    private String status;
    private String notes;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getMake() { return make; }
    public void setMake(String v) { this.make = v; }
    public String getModel() { return model; }
    public void setModel(String v) { this.model = v; }
    public String getSerial() { return serial; }
    public void setSerial(String v) { this.serial = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
