# Evidence Harbor — AI Assistant Instructions

Authoritative context for AI coding agents working in this repository. Read this before making changes.

---

## 1. Project Overview

**Evidence Harbor** is a JavaFX 21 desktop application for law-enforcement evidence management, impound tracking, and quartermaster workflows. It is an on-premises client that talks directly to a MariaDB server over JDBC. There is **no separate application server** — every installed client connects to the shared MariaDB instance, and all business logic runs inside the JavaFX process.

- **Language / runtime:** Java 21 (Microsoft Build of OpenJDK recommended)
- **UI:** JavaFX 21.0.5 (FXML + controllers + CSS theming)
- **Build:** Maven (`pom.xml`)
- **Database:** MariaDB 12.2.2 (JDBC driver `org.mariadb.jdbc:mariadb-java-client:3.4.1`)
- **Main class:** `com.evidenceharbor.app.MainApp`
- **Entry scripts:** `Evidence Harbor.bat`, `launch.vbs`, `splash.ps1`

### Run & build

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
$env:Path = "$env:JAVA_HOME\bin;C:\Users\CamdenPD - Deidre\.maven\maven-3.9.14\bin;$env:Path"
mvn -q javafx:run            # run
mvn clean compile            # recompile everything
mvn clean package            # build jar
```

After major FXML changes always `mvn clean` before launching — stale compiled FXML in `target/classes/fxml/` will mask edits in `src/main/resources/fxml/`.

---

## 2. Architecture — "Thick client, shared DB"

```
┌───────────────────────────────────────────────┐      ┌────────────────────┐
│  Evidence Harbor client (JavaFX)              │      │ MariaDB 12.2.2     │
│                                               │      │ database:          │
│  MainApp ─► LoginScreen ─► Navigator          │──┐   │   evidence_harbor  │
│    │                                          │  │   │                    │
│    ├─ ui/*Controller  (FXML-backed views)     │  │   │ Schema applied at  │
│    ├─ app/SessionManager  (auth + perms)      │  ├──►│ startup by Database│
│    ├─ domain/*           (POJO models)        │  │   │ Manager via        │
│    └─ persistence/*Repository ── JDBC ────────┘  │   │ schema-mariadb.sql │
│                                                  │   │ + seed-mariadb.sql │
└───────────────────────────────────────────────   │   └────────────────────┘
                                                   │
                                         (optional Tailscale VPN for remote sites)
```

- Every client process opens **its own** `Connection` through `DatabaseManager.getInstance()`.
- There is **no REST layer**, no "server" jar, no ORM. Repositories execute raw parameterized SQL.
- Multi-site deployments use **Tailscale** to expose the MariaDB host over a private VPN; the client points at the Tailscale IP in the first-time setup wizard.

### Package layout (`src/main/java/com/evidenceharbor/`)

| Package | Responsibility |
|---|---|
| `app/` | Bootstrap (`MainApp`), navigation (`Navigator`, `NavHelper`), session (`SessionManager`, `CurrentOfficerResolver`), setup/login screens, `DatabaseStartupScreen`, `SetupWizard`, `ComboBoxHelper` |
| `domain/` | Plain POJOs: `Evidence`, `Case`, `Officer`, `Person`, `Charge`, `BankAccount`, `BankTransaction`, `EvidenceAudit`, `AuditLog`, `LookupItem`, … |
| `persistence/` | `DatabaseManager` + one `*Repository` per aggregate. All SQL lives here. |
| `ui/<module>/` | FXML controllers grouped by module: `admin`, `cases`, `dropbox`, `evidence`, `inventory`, `people`, `reports`, `settings`, `shared` |
| `util/` | Cross-cutting helpers: `Dialogs` (themed popups), `TableExportUtil`, etc. |
| `tools/` | Developer / maintenance utilities |

### Resources (`src/main/resources/`)

- `fxml/*.fxml` — scene graphs; `fx:controller` points at a class in `ui/<module>/`.
- `sql/schema-mariadb.sql` — full schema (idempotent CREATE TABLE IF NOT EXISTS).
- `sql/seed-mariadb.sql` — default lookup values; re-run gated by `SEED_VERSION` in `DatabaseManager`.
- `sql/test-data-mariadb.sql` — optional dev seed.
- `styles/theme.css` — **single source of truth for dark-mode visuals.** Must be attached to every Dialog/Alert (see §6).

---

## 3. Database / "Server" Setup

There is no application server — the "server" is the MariaDB instance. Setup is fully automated from the client:

1. On first launch, `DatabaseStartupScreen` (or `SetupWizard`) prompts for host/port/db/user/password and writes `%USERPROFILE%\EvidenceHarbor\db.properties`.
2. `DatabaseManager` opens the JDBC connection, `CREATE DATABASE … IF NOT EXISTS` if permitted, runs `/sql/schema-mariadb.sql`, runs idempotent `runMigrations()` (ALTER TABLE … ADD COLUMN IF NOT EXISTS patterns), then applies the seed once per `SEED_VERSION`.
3. A `_meta` table tracks the applied seed version so reseeding is skipped on subsequent runs.
4. `DatabaseManager.getInstance()` is a **singleton** — use it everywhere; do not open your own `DriverManager.getConnection`.

### SQL rules (enforced across the codebase)

- **MariaDB dialect only.** Never write SQLite syntax (no `AUTOINCREMENT`, no `REPLACE INTO` substitutes, no `PRAGMA`).
- Use `INSERT IGNORE` / `UPDATE … ` — never `INSERT OR IGNORE`.
- Migrations in `runMigrations()` must be idempotent (`ALTER TABLE … ADD COLUMN IF NOT EXISTS`, check `INFORMATION_SCHEMA` before mutating).
- Always use `PreparedStatement` with `?` placeholders. Never concatenate user input into SQL.
- Blank `TextField`s should be persisted as `NULL`, not empty strings — see `PersonRepository.save(...)` for the canonical pattern.

### Config file

`%USERPROFILE%\EvidenceHarbor\db.properties`:

```properties
host=127.0.0.1
port=3306
database=evidence_harbor
user=root
password=…
```

Delete this file to re-trigger the setup wizard.

---

## 4. Runtime Flow

1. `MainApp.start()` → splash → `DatabaseStartupScreen` verifies JDBC connectivity.
2. If no config: `FirstTimeSetupScreen` + `SetupWizard` create `db.properties` and seed the agency.
3. `LoginScreen` — **PBKDF2** password auth against `officers` table (`password_hash`, `password_salt`, iteration count stored per-row).
4. `SessionManager` caches the logged-in `Officer` and computes effective permissions once.
5. `Navigator` swaps scenes. Every navigation target is permission-gated; unauthorized nav redirects to the role's default page.
6. Controllers fetch data lazily in `initialize()` or when the view is shown, always through a `*Repository`.

---

## 5. Permissions

Roles: `admin`, `evidence_tech`, `officer`.

```
effective = role_defaults + granted_overrides − revoked_overrides
```

Gate **both** nav visibility and runtime actions with `SessionManager.can(flag)`. Key flags:

| Flag | Guards |
|---|---|
| `can_view_all_evidence` | Inventory, Reports, Dropbox Check-in |
| `can_manage_users` | User Management, Permission Management |
| `can_view_audit` | Audit Trail |
| `can_manage_settings` | Settings, Lookup Admin |

Never rely on the DB to enforce permissions — enforce in Java before dispatching SQL.

---

## 6. UI Conventions — **critical for agents**

### Dark-mode theming

The app is dark-themed. Any `Alert`/`Dialog` created without attaching `/styles/theme.css` will render as a light native dialog and look broken. **Always** go through `com.evidenceharbor.util.Dialogs`:

```java
Dialogs.info("Saved", "Record saved.");
Dialogs.warn("Missing Information", "Name is required.");
Dialogs.error(ex);                              // Throwable overload
if (!Dialogs.confirm("Delete?", "Cannot be undone.")) return;

// Custom Dialog<T>? still required — just style it:
Dialog<MyResult> dlg = new Dialog<>();
Dialogs.style(dlg);
```

Do **not** write `new Alert(AlertType.X, …)` directly. If you find one during edits, convert it.

Authoritative color tokens (from `theme.css`):

| Token | Hex | Usage |
|---|---|---|
| scene bg | `#1a2235` | page background |
| card | `#1e2d40` | dialog-pane, elevated cards |
| elevated | `#243447` | header-panel, hovered buttons base |
| border | `#2d4059` | hover, strokes |
| text | `#e2e8f0` | primary text |
| muted | `#94a3b8` | secondary/labels |
| primary | `#3b82f6` | accent / primary buttons |
| danger | `#ef4444` / `#7f1d1d` | destructive |
| warn | `#f6ad55` | warnings |
| accent L / R | `#38bdf8` / `#a78bfa` | combined-control highlights |

### Searchable ComboBoxes

`CaseDetailController.makeSearchableComboBox` is the canonical implementation. When editing:

- **Never** reset a `FilteredList` predicate from inside the `valueProperty` listener — it clears the selection and re-fires with `null`, blanking the editor.
- Reset predicates only on `showingProperty` change and on **focus loss** (and preserve typed editor text).
- Mutate the **backing** `ObservableList`; `FilteredList` is unmodifiable.
- Treat `value == null` as a no-op in the text setter; guard re-entrant updates with a `programmaticTextUpdate` boolean flag.

### Controllers & FXML

- One FXML per view, `fx:controller` fully qualified to `com.evidenceharbor.ui.<module>.XController`.
- Every FXML `onAction="#foo"` must have a matching `@FXML private void foo()` in the controller — a missing handler throws `LoadException` at scene swap time.
- Nav bars are included as sub-FXML fragments (e.g. `DbStatusBar.fxml`); shared nav handlers live on every module controller (`onCases`, `onInventory`, `onPeople`, `onDropbox`, `onReports`, `onSettings`, `onAdmin`, `onDashboard`, `onBack`, …). When adding a new module, add handlers everywhere or nav will break.

---

## 7. Dropbox Workflow

A dropbox session is only valid when ≥ 1 evidence row has `status = 'In Dropbox'` **and** a valid dropbox `storage_location`. Each finished session writes to `dropbox_sessions` and emits chain-of-custody rows in `chain_of_custody`. See `DropboxController` + `ChainOfCustodyRepository`.

---

## 8. Audit Logging

Mutating actions should write to `audit_log` via `AuditLogRepository`. Keep the `module`, `action`, `entity_type`, `entity_id`, and `details` fields populated so the Audit Trail view is useful. Deletions must be logged **before** the DELETE statement (so the entity is still resolvable for `details`).

---

## 9. Coding Conventions

- Java 21 features are fine (records, switch expressions, pattern matching) — prefer them where they clarify intent.
- Keep controllers thin: UI wiring + calls into repositories. No SQL in controllers.
- Repositories throw `SQLException`; controllers catch and route to `Dialogs.error(e)` (or a local `showError`).
- **Never** swallow exceptions silently. At minimum `e.printStackTrace()` in a catch and surface via `Dialogs.error`.
- Don't add docstrings, comments, or refactors to code you aren't modifying.
- Don't create markdown change-logs unless explicitly asked.
- Prefer editing existing files over creating new ones; especially, don't add a new util class if an existing one fits.
- **Always keep the todo list current.** Use `manage_todo_list` at the start of any multi-step task, mark exactly one item `in-progress` before working on it, and flip it to `completed` immediately after it's done — never batch completions. Skip the todo list only for single trivial edits.

---

## 10. Common Pitfalls (learned from prior sessions)

- **Blank/white dialog?** You used a raw `Alert`. Switch to `Dialogs.*`.
- **ComboBox selection disappears on pick?** A value listener is resetting the `FilteredList` predicate. Move it to `showingProperty` / focus-loss and guard against `null` values.
- **`LoadException: No such method onXxx`** after nav changes? Add the `@FXML` stub on every controller referenced by that FXML nav bar.
- **Migration fails on second run?** Missing `IF NOT EXISTS` — make the ALTER idempotent.
- **`FilteredList` throws `UnsupportedOperationException`?** Mutate the source list, not the filtered view.
- **Stale UI after FXML edit?** `mvn clean` before running; `target/classes/fxml/*.fxml` is served instead of the edited source otherwise.

---

## 11. Where to look first

| Task | Start here |
|---|---|
| Add a nav destination | `Navigator.java`, then every controller with a nav bar |
| Add a DB column | `schema-mariadb.sql` + idempotent ALTER in `DatabaseManager.runMigrations()` + domain POJO + repository |
| New popup/dialog | `util/Dialogs.java` (extend if missing a variant) |
| New permission flag | seed SQL, `SessionManager`, `PermissionManagementController`, gate call-sites |
| New module | FXML in `resources/fxml/`, controller in `ui/<module>/`, register in `Navigator`, add nav handlers everywhere, add permission flag if needed |
