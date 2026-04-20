# Evidence Harbor (Java)

Evidence Harbor is a JavaFX desktop application for law enforcement evidence management, impound tracking, and quartermaster workflows. It is free to use, actively developed, and designed for any police department needing a lightweight on-premises solution.

## License

Licensed under the Evidence Harbor Free Use, No Resale License.

- Free to use by police departments and other public safety/government users
- Permitted to modify for internal use
- Redistribution allowed on a non-commercial basis only
- Selling or commercial resale requires written permission
- Provided as-is, no warranty

See the LICENSE file for full terms.

## Tech Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| JavaFX | 21.0.5 |
| Build | Maven |
| Database | MariaDB |

## Project Structure

```
src/main/java/com/evidenceharbor/
  app/          # Bootstrap, navigation, session, permissions, shared helpers
  domain/       # Domain models (Evidence, Case, Officer, Person, Charge, ...)
  persistence/  # Repository classes and DatabaseManager
  ui/           # JavaFX controllers organized by module
    admin/      # Admin dashboard, user management, permissions, audit trail,
                #   lookup admin, bank account ledger, evidence audit
    cases/      # Case list, case detail, add evidence
    dropbox/    # Dropbox check-in workflow
    evidence/   # Evidence detail and intake
    inventory/  # Inventory view
    people/     # People management
    reports/    # Reports
    settings/   # Settings
    shared/     # Shared UI components
  tools/        # Utility tools
  util/         # General utilities
src/main/resources/
  fxml/         # JavaFX layout files
  sql/          # Schema and seed scripts
  styles/       # Global CSS themes
```

## Modules

### Evidence
- **Cases** — create and manage cases, attach charges, link people (victims, suspects, witnesses, etc.), add evidence
- **Inventory** — full evidence inventory with search and status tracking
- **People** — person records with role associations across cases
- **Dropbox Check-in** — officer dropbox session workflow; processes items with status `In Dropbox`
- **Reports** — evidence and case reporting
- **Add Evidence** — barcode-driven intake with chain of custody

### Admin
- **Admin Dashboard** — agency overview
- **User Management** — create/edit officers with role and password management
- **Permission Management** — per-officer permission overrides on top of role defaults
- **Audit Trail** — system-wide audit log
- **Lookup Admin** — manage lookup tables (evidence types, storage locations, statuses, etc.)
- **Bank Account Ledger** — track agency fund accounts and transactions
- **Evidence Audit** — evidence audit records

### Impound
- **Impound Lot** — vehicle impound intake and tracking


## Permission System

Roles: `admin`, `evidence_tech`, `officer`

Each role has default permission flags. Admins can add or remove individual flags per officer. Effective permissions are computed at login as:

```
effective = role_defaults + granted_overrides − revoked_overrides
```

Permission flags gate both navigation visibility and runtime access checks via `SessionManager.can(flag)`.

Notable gating:
- `can_view_all_evidence` — required to access Inventory, Reports, and Dropbox Check-in
- `can_manage_users` — required for User Management and Permission Management
- `can_view_audit` — required for Audit Trail
- `can_manage_settings` — required for Settings and Lookup Admin

## Navigation Pattern

Two-level layout:

1. **Top row** — module tabs: `Evidence`, `Impound`, `Quartermaster`
2. **Secondary row** — workspace tabs within the active module

Navigation is enforced server-side in `Navigator.java` — unauthorized navigation attempts redirect to the default page for the user's role.

## Startup Flow

1. `DatabaseStartupScreen` — verifies DB connection
2. `FirstTimeSetupScreen` / `SetupWizard` — runs on first launch to configure agency settings
3. `LoginScreen` — PBKDF2 password authentication
4. Main workspace loads with nav visibility applied per effective permissions

## Dropbox Workflow

Sessions only activate when at least one evidence item has status `In Dropbox` at a valid dropbox location. Each session is logged to `dropbox_sessions` and generates chain-of-custody records.

## Database

Schema and seed data are auto-applied at startup by `DatabaseManager`.

- Schema: `src/main/resources/sql/schema-mariadb.sql`
- Seed data: `src/main/resources/sql/seed-mariadb.sql`

### Required Downloads

These are not included in the repository. Download and install before running:

| Software | Version | Download |
|----------|---------|----------|
| MariaDB | 12.2.2 | https://mariadb.org/download/ |
| Tailscale | 1.96.3 | https://tailscale.com/download/windows |

## Build and Run

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
mvn clean javafx:run
```

## Packaging

```powershell
mvn clean package
```

Main class: `com.evidenceharbor.app.MainApp`

## Notes

- `.gitignore` excludes build output and local DB artifacts.
- After major FXML changes, run `mvn clean` before launch to clear stale compiled resources.
- All ComboBoxes in the UI are type-searchable (filter as you type).
