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
}
