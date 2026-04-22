package com.evidenceharbor.domain;

public class BankTransaction {
    private int id;
    private int accountId;
    private String action;
    private double amount;
    private String slipNumber;
    private String date;
    private String performedBy;
    private String notes;
    private String sourceRef;
    private boolean voided;
    private String voidedReason;
    private String voidedBy;
    private String voidedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getAccountId() { return accountId; }
    public void setAccountId(int v) { this.accountId = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public double getAmount() { return amount; }
    public void setAmount(double v) { this.amount = v; }
    public String getSlipNumber() { return slipNumber; }
    public void setSlipNumber(String v) { this.slipNumber = v; }
    public String getDate() { return date; }
    public void setDate(String v) { this.date = v; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String v) { this.performedBy = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String v) { this.sourceRef = v; }
    public boolean isVoided() { return voided; }
    public void setVoided(boolean v) { this.voided = v; }
    public String getVoidedReason() { return voidedReason; }
    public void setVoidedReason(String v) { this.voidedReason = v; }
    public String getVoidedBy() { return voidedBy; }
    public void setVoidedBy(String v) { this.voidedBy = v; }
    public String getVoidedAt() { return voidedAt; }
    public void setVoidedAt(String v) { this.voidedAt = v; }
}
