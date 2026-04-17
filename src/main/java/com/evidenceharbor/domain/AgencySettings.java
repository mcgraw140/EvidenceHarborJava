package com.evidenceharbor.domain;

public class AgencySettings {

    private String agencyName       = "";
    private String agencyAddress    = "";
    private String agencyCity       = "";
    private String agencyState      = "";
    private String agencyZip        = "";
    private String caseNumberPattern  = "";  // regex pattern for validation
    private String caseNumberExample  = "";  // human-readable example
    private String evidenceNumberPattern = "";
    private String evidenceNumberExample = "";
    private String barcodePrefix      = "";  // prefix for Code 128 barcode generation

    public String getAgencyName()             { return agencyName; }
    public void   setAgencyName(String v)     { agencyName = v == null ? "" : v; }

    public String getAgencyAddress()          { return agencyAddress; }
    public void   setAgencyAddress(String v)  { agencyAddress = v == null ? "" : v; }

    public String getAgencyCity()             { return agencyCity; }
    public void   setAgencyCity(String v)     { agencyCity = v == null ? "" : v; }

    public String getAgencyState()            { return agencyState; }
    public void   setAgencyState(String v)    { agencyState = v == null ? "" : v; }

    public String getAgencyZip()              { return agencyZip; }
    public void   setAgencyZip(String v)      { agencyZip = v == null ? "" : v; }

    public String getCaseNumberPattern()      { return caseNumberPattern; }
    public void   setCaseNumberPattern(String v) { caseNumberPattern = v == null ? "" : v; }

    public String getCaseNumberExample()      { return caseNumberExample; }
    public void   setCaseNumberExample(String v) { caseNumberExample = v == null ? "" : v; }

    public String getEvidenceNumberPattern()  { return evidenceNumberPattern; }
    public void   setEvidenceNumberPattern(String v) { evidenceNumberPattern = v == null ? "" : v; }

    public String getEvidenceNumberExample()  { return evidenceNumberExample; }
    public void   setEvidenceNumberExample(String v) { evidenceNumberExample = v == null ? "" : v; }

    public String getBarcodePrefix()          { return barcodePrefix; }
    public void   setBarcodePrefix(String v)  { barcodePrefix = v == null ? "" : v; }
}
