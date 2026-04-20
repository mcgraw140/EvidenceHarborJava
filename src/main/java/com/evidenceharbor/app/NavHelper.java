package com.evidenceharbor.app;

import javafx.scene.control.Button;

/**
 * Applies page-level nav visibility based on the current session's permissions.
 * Call {@link #applyNavVisibility} at the end of each controller's {@code initialize()}.
 * Pass {@code null} for any button that does not exist on that page.
 */
public final class NavHelper {

    private NavHelper() {}

    /**
     * Hides (and unmanages) nav buttons the current user does not have access to.
     *
     * @param navAdminTab      Top-bar "Admin" tab
     * @param navAuditTrailBtn Sub-nav "Audit Trail" button (admin pages)
     * @param navSettingsBtn   Sub-nav "Settings" button (admin pages)
     * @param navInventoryBtn  Sub-nav "Inventory" button (evidence pages)
     * @param navReportsBtn    Sub-nav "Reports" button (evidence pages)
     * @param navDropboxBtn    Sub-nav "Dropbox Check-in" button (evidence tech+ only)
     */
    public static void applyNavVisibility(
            Button navAdminTab,
            Button navAuditTrailBtn,
            Button navSettingsBtn,
            Button navInventoryBtn,
            Button navReportsBtn,
            Button navDropboxBtn) {

        boolean hasAnyAdminAccess = SessionManager.can("can_manage_users")
                || SessionManager.can("can_manage_settings")
                || SessionManager.can("can_view_audit_logs");

        setVisible(navAdminTab,      hasAnyAdminAccess);
        setVisible(navAuditTrailBtn, SessionManager.can("can_view_audit_logs"));
        setVisible(navSettingsBtn,   SessionManager.can("can_manage_settings"));
        setVisible(navInventoryBtn,  SessionManager.can("can_view_all_evidence"));
        setVisible(navReportsBtn,    SessionManager.can("can_view_all_evidence"));
        setVisible(navDropboxBtn,    SessionManager.can("can_view_all_evidence"));
    }

    private static void setVisible(Button btn, boolean visible) {
        if (btn == null) return;
        btn.setVisible(visible);
        btn.setManaged(visible);
    }
}
