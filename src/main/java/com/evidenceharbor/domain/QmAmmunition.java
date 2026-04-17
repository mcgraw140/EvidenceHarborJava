package com.evidenceharbor.domain;

public class QmAmmunition {
    private int id;
    private String caliber;
    private int quantity;
    private String location;
    private String updatedAt;
    private String notes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCaliber() { return caliber; }
    public void setCaliber(String v) { this.caliber = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String v) { this.updatedAt = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
}
