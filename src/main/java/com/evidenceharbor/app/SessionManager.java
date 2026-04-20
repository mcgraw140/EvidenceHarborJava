package com.evidenceharbor.app;

import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.ui.admin.PermissionManagementController;

import java.util.Set;

/**
 * Holds the currently authenticated officer for the session.
 * Cleared on logout.
 */
public final class SessionManager {

    private static Officer currentOfficer;
    private static Set<String> effectivePermissions;

    private SessionManager() {}

    public static Officer getCurrentOfficer() {
        return currentOfficer;
    }

    public static void setCurrentOfficer(Officer officer) {
        currentOfficer = officer;
        effectivePermissions = officer != null
                ? PermissionManagementController.computeEffective(officer)
                : Set.of();
    }

    public static boolean isLoggedIn() {
        return currentOfficer != null;
    }

    public static boolean isAdmin() {
        return currentOfficer != null && "admin".equalsIgnoreCase(currentOfficer.getRole());
    }

    public static boolean isEvidenceTech() {
        return currentOfficer != null && "evidence_tech".equalsIgnoreCase(currentOfficer.getRole());
    }

    /**
     * Returns true if the current user has the given permission flag.
     * Effective permissions = role defaults + add overrides - remove overrides.
     */
    public static boolean can(String permissionFlag) {
        return effectivePermissions != null && effectivePermissions.contains(permissionFlag);
    }

    public static void clear() {
        currentOfficer = null;
        effectivePermissions = null;
    }
}
