package com.evidenceharbor.domain;

public class QmVehicleImpound {
    private int id;
    private int caseId;
    private String make;
    private String model;
    private String year;
    private String vin;
    private String plate;
    private String color;
    private String impoundDate;
    private String releaseDate;
    private String status;
    private String reason;
    private String notes;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCaseId() { return caseId; }
    public void setCaseId(int caseId) { this.caseId = caseId; }
    public String getMake() { return make; }
    public void setMake(String v) { this.make = v; }
    public String getModel() { return model; }
    public void setModel(String v) { this.model = v; }
    public String getYear() { return year; }
    public void setYear(String v) { this.year = v; }
    public String getVin() { return vin; }
    public void setVin(String v) { this.vin = v; }
    public String getPlate() { return plate; }
    public void setPlate(String v) { this.plate = v; }
    public String getColor() { return color; }
    public void setColor(String v) { this.color = v; }
    public String getImpoundDate() { return impoundDate; }
    public void setImpoundDate(String v) { this.impoundDate = v; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String v) { this.releaseDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
