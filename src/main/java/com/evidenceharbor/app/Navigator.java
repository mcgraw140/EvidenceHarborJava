package com.evidenceharbor.app;

import com.evidenceharbor.domain.Case;
import com.evidenceharbor.ui.cases.CaseDetailController;
import com.evidenceharbor.ui.evidence.AddEvidenceController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Navigator {

    private final Stage stage;
    private static Navigator instance;

    public Navigator(Stage stage) {
        this.stage = stage;
        instance = this;
        stage.setTitle("Evidence Harbor");
        stage.setWidth(1280);
        stage.setHeight(780);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
    }

    public static Navigator get() { return instance; }

    public void showCaseList() {
        loadScene("/fxml/CaseList.fxml", null);
    }

    public void showCaseListAndCreate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CaseList.fxml"));
            Parent root = loader.load();
            applyScene(root);
            com.evidenceharbor.ui.cases.CaseListController ctrl = loader.getController();
            javafx.application.Platform.runLater(ctrl::onNewCase);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showInventory() {
        if (!SessionManager.can("can_view_all_evidence")) { showCaseList(); return; }
        loadScene("/fxml/Inventory.fxml", null);
    }

    public void showPeople() {
        loadScene("/fxml/People.fxml", null);
    }

    public void showDropbox() {
        if (!SessionManager.can("can_view_all_evidence")) { showCaseList(); return; }
        loadScene("/fxml/Dropbox.fxml", null);
    }

    public void showReports() {
        if (!SessionManager.can("can_view_all_evidence")) { showCaseList(); return; }
        loadScene("/fxml/Reports.fxml", null);
    }

    public void showSettings() {
        if (!SessionManager.can("can_manage_settings")) { showAdminDashboard(); return; }
        loadScene("/fxml/Settings.fxml", null);
    }

    public void showUserManagement() {
        if (!SessionManager.can("can_manage_users")) { showAdminDashboard(); return; }
        loadScene("/fxml/UserManagement.fxml", null);
    }

    public void showAdminDashboard() {
        boolean hasAdminAccess = SessionManager.can("can_manage_users")
                || SessionManager.can("can_manage_settings")
                || SessionManager.can("can_view_audit_logs");
        if (!hasAdminAccess) { showCaseList(); return; }
        loadScene("/fxml/AdminDashboard.fxml", null);
    }

    public void showPermissions(com.evidenceharbor.domain.Officer officer) {
        if (!SessionManager.can("can_manage_users")) { showAdminDashboard(); return; }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PermissionManagement.fxml"));
        try {
            Parent root = loader.load();
            com.evidenceharbor.ui.admin.PermissionManagementController ctrl = loader.getController();
            ctrl.setOfficer(officer);
            applyScene(root);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public void showLookupAdmin() {
        if (!SessionManager.can("can_manage_settings")) { showAdminDashboard(); return; }
        loadScene("/fxml/LookupAdmin.fxml", null);
    }

    public void showEvidenceAudit() {
        if (!SessionManager.can("can_view_audit_logs")) { showAdminDashboard(); return; }
        loadScene("/fxml/EvidenceAudit.fxml", null);
    }

    public void showEvidenceAuditSession(com.evidenceharbor.domain.EvidenceAudit audit) {
        if (!SessionManager.can("can_view_audit_logs")) { showAdminDashboard(); return; }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EvidenceAuditSession.fxml"));
        try {
            Parent root = loader.load();
            com.evidenceharbor.ui.admin.EvidenceAuditSessionController ctrl = loader.getController();
            ctrl.setAudit(audit);
            applyScene(root);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public void showAuditTrail() {
        if (!SessionManager.can("can_view_audit_logs")) { showAdminDashboard(); return; }
        loadScene("/fxml/AuditTrail.fxml", null);
    }

    public void showBankAccountLedger() {
        if (!SessionManager.isAdmin() && !SessionManager.can("can_manage_settings")) {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING,
                    "You do not have permission to access the Bank Ledger.\n\n"
                            + "Required: can_manage_settings",
                    javafx.scene.control.ButtonType.OK);
            a.setHeaderText("Access Denied");
            a.showAndWait();
            return;
        }
        loadScene("/fxml/BankAccountLedger.fxml", null);
    }

    public void showImpoundLot() {
        loadScene("/fxml/ImpoundLot.fxml", null);
    }

    public void showEvidenceDashboard() {
        loadScene("/fxml/EvidenceDashboard.fxml", null);
    }

    public void showCaseDetail(Case c) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CaseDetail.fxml"));
        try {
            Parent root = loader.load();
            CaseDetailController ctrl = loader.getController();
            ctrl.setCase(c);
            applyScene(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showAddEvidence(Case c) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddEvidence.fxml"));
        try {
            Parent root = loader.load();
            AddEvidenceController ctrl = loader.getController();
            ctrl.initForCase(c);
            applyScene(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadScene(String fxml, Object unused) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            applyScene(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyScene(Parent root) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        stage.show();
    }
}
