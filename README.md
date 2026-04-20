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

Schema and seed data are applied automatically at startup by `DatabaseManager` using the MariaDB scripts in `src/main/resources/sql/`.

## Dependencies & Installation

This section covers everything needed to build and run Evidence Harbor from source on Windows.

---

### 1. Java Development Kit (JDK) 21

Evidence Harbor requires **Java 21**. The Microsoft Build of OpenJDK is recommended.

**Download:** https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-21

**Install:**
1. Run the installer (`.msi`)
2. Note the install path (e.g. `C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`)
3. Set `JAVA_HOME` before running Maven:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
```

To make this permanent, add it to your system environment variables.

**Verify:**
```powershell
java -version
# should output: openjdk version "21.x.x"
```

---

### 2. Apache Maven

Maven handles compilation, dependency resolution, and running the app.

**Download:** https://maven.apache.org/download.cgi — download the binary zip (e.g. `apache-maven-3.9.9-bin.zip`)

**Install:**
1. Extract to a stable location (e.g. `C:\Users\<you>\Maven\apache-maven-3.9.9`)
2. Add `bin\` to your PATH, or call it directly:

```powershell
& "C:\Users\<you>\Maven\apache-maven-3.9.9\bin\mvn.cmd" --version
```

**Verify:**
```powershell
mvn --version
# should output: Apache Maven 3.9.x
```

---

### 3. MariaDB 12.2.2

Evidence Harbor connects to a local or networked MariaDB instance.

**Download:** https://mariadb.org/download/ — select version 12.2.2, Windows x64 MSI

**Install:**
1. Run the `.msi` installer
2. During setup, set a root password (or leave blank for local dev)
3. Default port is `3306` — leave as-is unless you have a conflict
4. Enable the **MariaDB** Windows service so it starts automatically

**After install, create the database:**
```sql
CREATE DATABASE evidence_harbor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

You can run this in the MariaDB command-line client:
```powershell
mysql -u root -p
# then paste the CREATE DATABASE statement above
```

The schema and seed data are applied automatically by the app on first launch — you do not need to run SQL scripts manually.

**Default connection settings** (configured via the first-time setup wizard on first launch):

| Setting | Default |
|---------|---------|
| Host | `127.0.0.1` |
| Port | `3306` |
| Database | `evidence_harbor` |
| User | `root` |
| Password | *(blank or whatever you set during MariaDB install)* |

Connection settings are saved to `%USERPROFILE%\EvidenceHarbor\db.properties` after first setup.

---

### 4. Tailscale (Optional — for remote/multi-site access)

Tailscale is used to securely connect officers at remote sites to a central MariaDB server over a private VPN without opening firewall ports.

**Download:** https://tailscale.com/download/windows — version 1.96.3

**Install:**
1. Run the installer
2. Log in with a Tailscale account (free tier supports small teams)
3. On the server machine running MariaDB, note the Tailscale IP (e.g. `100.x.x.x`)
4. On client machines, set the MariaDB host in the Evidence Harbor setup wizard to that Tailscale IP

This is optional — if all machines are on the same local network, Tailscale is not needed.

---

### 5. Maven Dependencies (auto-resolved)

These are declared in `pom.xml` and downloaded automatically by Maven on first build. No manual installation required.

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.openjfx:javafx-controls` | 21.0.5 | JavaFX UI controls |
| `org.openjfx:javafx-fxml` | 21.0.5 | JavaFX FXML layout loading |
| `org.mariadb.jdbc:mariadb-java-client` | 3.4.1 | MariaDB JDBC driver |
| `org.junit.jupiter:junit-jupiter` | 5.10.2 | Unit testing (test scope only) |

---

### 6. Required Downloads Summary

| Software | Version | Required | Download |
|----------|---------|----------|----------|
| JDK 21 | 21.x | Yes | https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-21 |
| Apache Maven | 3.9.x | Yes | https://maven.apache.org/download.cgi |
| MariaDB | 12.2.2 | Yes | https://mariadb.org/download/ |
| Tailscale | 1.96.3 | No | https://tailscale.com/download/windows |

---

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
