# Evidence Harbor (Java)

Evidence Harbor is a JavaFX desktop application for evidence, impound, and quartermaster workflows.

This project is free to use and is still in active development. It is intended for any police department looking for a simple system.

## License

This project is licensed under the Evidence Harbor Free Use, No Resale License.

- Free to use by police departments and other public safety/government users
- Allowed to modify for internal use
- Redistribution is allowed only on a non-commercial basis
- Selling or commercial resale is not allowed without written permission
- No warranty (provided as-is)

See the LICENSE file for full terms.

## Tech Stack

- Java 21
- JavaFX 21.0.5
- Maven
- SQLite (`sqlite-jdbc 3.47.1.0`)

## Project Structure

- `src/main/java/com/evidenceharbor/app`: app bootstrap and navigation
- `src/main/java/com/evidenceharbor/domain`: domain models
- `src/main/java/com/evidenceharbor/persistence`: repositories and DB management
- `src/main/java/com/evidenceharbor/ui`: JavaFX controllers by module
- `src/main/resources/fxml`: JavaFX views
- `src/main/resources/sql`: schema and seed scripts
- `src/main/resources/styles`: global styles

## Modules

- Evidence
	- Cases
	- Inventory
	- People
	- Dropbox Check-in
	- Reports
	- Settings/Admin workflows
- Impound
	- Impound Lot
- Quartermaster
	- Dashboard
	- Assign Equipment
	- Ammunition
	- Inventory Levels
	- Inventory Audit
	- Officer Loadouts
	- Vehicle Impound

## Navigation Pattern

The app uses a two-level navigation layout:

1. Top row: module-level tabs (`Evidence`, `Impound`, `Quartermaster`, `Narcotics`)
2. Secondary row: module-specific workspace tabs

## Dropbox Workflow Guard

Dropbox check-in sessions only start when there is at least one evidence item currently in an approved dropbox location with status `In Dropbox`.

## Vehicle Workflow

Vehicles are handled as impound records from case workflows and impound/quartermaster flows, not as standard evidence intake items.

## Build and Run

From repository root:

```powershell
mvn clean compile
mvn javafx:run
```

If multiple JDKs are installed, set `JAVA_HOME` first:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
mvn clean javafx:run
```

## Packaging

Create a shaded JAR:

```powershell
mvn clean package
```

Main class is `com.evidenceharbor.app.MainApp`.

## Database

Database is initialized at startup by `DatabaseManager` using:

- `src/main/resources/sql/schema.sql`
- `src/main/resources/sql/seed.sql`

Runtime DB file defaults to:

- `%USERPROFILE%\EvidenceHarbor\evidence_harbor.db`

## Notes

- `.gitignore` excludes build output and local DB artifacts.
- Temporary runtime failures after major FXML changes can often be resolved by running `mvn clean` before launch.
