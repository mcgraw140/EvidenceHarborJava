package com.evidenceharbor.ui.admin;

import com.evidenceharbor.app.Navigator;
import javafx.fxml.FXML;

public class AdminDashboardController {

    // ── Admin card actions ────────────────────────────────────────────────────
    @FXML private void onUserManagement()    { Navigator.get().showUserManagement(); }
    @FXML private void onLookupAdministration() { Navigator.get().showLookupAdmin(); }
    @FXML private void onEvidenceAudit()     { Navigator.get().showEvidenceAudit(); }
    @FXML private void onAuditTrail()        { Navigator.get().showAuditTrail(); }
    @FXML private void onBankAccountLedger() { Navigator.get().showBankAccountLedger(); }

    // ── Nav bar ───────────────────────────────────────────────────────────────
    @FXML private void onCases()     { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople()    { Navigator.get().showPeople(); }
    @FXML private void onDropbox()   { Navigator.get().showDropbox(); }
    @FXML private void onReports()   { Navigator.get().showReports(); }
    @FXML private void onSettings()  { Navigator.get().showSettings(); }
    @FXML private void onAdmin()          { /* already here */ }
    @FXML private void onQuartermaster()  { Navigator.get().showQmDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }}
