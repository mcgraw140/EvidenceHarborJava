package com.evidenceharbor.domain;

public class QmAssignment {
    private int id;
    private int equipmentId;
    private String equipmentName;
    private String equipmentCategory;
    private String equipmentSerial;
    private String officerName;
    private String assignedDate;
    private String returnedDate;
    private String notes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEquipmentId() { return equipmentId; }
    public void setEquipmentId(int v) { this.equipmentId = v; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String v) { this.equipmentName = v; }
    public String getEquipmentCategory() { return equipmentCategory; }
    public void setEquipmentCategory(String v) { this.equipmentCategory = v; }
    public String getEquipmentSerial() { return equipmentSerial; }
    public void setEquipmentSerial(String v) { this.equipmentSerial = v; }
    public String getOfficerName() { return officerName; }
    public void setOfficerName(String v) { this.officerName = v; }
    public String getAssignedDate() { return assignedDate; }
    public void setAssignedDate(String v) { this.assignedDate = v; }
    public String getReturnedDate() { return returnedDate; }
    public void setReturnedDate(String v) { this.returnedDate = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
}
