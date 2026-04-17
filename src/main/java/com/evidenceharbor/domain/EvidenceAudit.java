package com.evidenceharbor.domain;

public class EvidenceAudit {
    private int id;
    private String createdAt;
    private String completedAt;
    private String auditType;   // Full, Random, Location
    private String scope;       // e.g. "100%" or location name
    private String createdBy;
    private String status;      // In Progress, Completed
    private String itemsJson;

    public EvidenceAudit() {}

    public int    getId()           { return id; }
    public void   setId(int id)     { this.id = id; }
    public String getCreatedAt()    { return createdAt; }
    public void   setCreatedAt(String t)  { this.createdAt = t; }
    public String getCompletedAt()  { return completedAt; }
    public void   setCompletedAt(String t){ this.completedAt = t; }
    public String getAuditType()    { return auditType; }
    public void   setAuditType(String t)  { this.auditType = t; }
    public String getScope()        { return scope; }
    public void   setScope(String s){ this.scope = s; }
    public String getCreatedBy()    { return createdBy; }
    public void   setCreatedBy(String c)  { this.createdBy = c; }
    public String getStatus()       { return status; }
    public void   setStatus(String s){ this.status = s; }
    public String getItemsJson()    { return itemsJson; }
    public void   setItemsJson(String j){ this.itemsJson = j; }
}
