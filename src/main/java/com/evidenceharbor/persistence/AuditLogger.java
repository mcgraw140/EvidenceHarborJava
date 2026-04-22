package com.evidenceharbor.persistence;

import com.evidenceharbor.app.SessionManager;
import com.evidenceharbor.domain.Officer;

/**
 * Small, safe facade around {@link AuditLogRepository}. Failures are swallowed
 * so audit logging never breaks the main operation. The current actor is
 * resolved from {@link SessionManager} when available, otherwise falls back to
 * "system".
 */
public final class AuditLogger {

    private static final AuditLogRepository REPO = new AuditLogRepository();

    private AuditLogger() {}

    public static void log(String module, String action, String entityType,
                           String entityId, String details) {
        try {
            REPO.insert(currentUser(), action, module, entityType, entityId, details);
        } catch (Exception e) {
            // Never break the main operation because of audit logging.
            e.printStackTrace();
        }
    }

    public static String currentUser() {
        try {
            Officer o = SessionManager.getCurrentOfficer();
            if (o != null && o.getName() != null && !o.getName().isBlank()) {
                return o.getName();
            }
        } catch (Throwable ignored) {}
        return "system";
    }
}
