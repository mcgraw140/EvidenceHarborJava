package com.evidenceharbor.domain;

public class AuditLog {
    private int id;
    private String timestamp;
    private String userName;
    private String action;
    private String module;
    private String entityType;
    private String entityId;
    private String details;

    public AuditLog() {}

    public int    getId()        { return id; }
    public void   setId(int id)  { this.id = id; }
    public String getTimestamp() { return timestamp; }
    public void   setTimestamp(String t) { this.timestamp = t; }
    public String getUserName()  { return userName; }
    public void   setUserName(String u) { this.userName = u; }
    public String getAction()    { return action; }
    public void   setAction(String a) { this.action = a; }
    public String getModule()    { return module; }
    public void   setModule(String m) { this.module = m; }
    public String getEntityType(){ return entityType; }
    public void   setEntityType(String e) { this.entityType = e; }
    public String getEntityId()  { return entityId; }
    public void   setEntityId(String e) { this.entityId = e; }
    public String getDetails()   { return details; }
    public void   setDetails(String d) { this.details = d; }
}
