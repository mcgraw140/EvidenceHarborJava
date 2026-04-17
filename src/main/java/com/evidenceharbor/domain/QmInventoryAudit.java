package com.evidenceharbor.domain;

public class QmInventoryAudit {
    private int id;
    private String createdAt;
    private String completedAt;
    private String createdBy;
    private String status;
    private String notes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String v) { this.completedAt = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
}
