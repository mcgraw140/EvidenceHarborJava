-- Evidence Harbor Schema

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS officers (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    name    TEXT NOT NULL UNIQUE,
    badge   TEXT
);

CREATE TABLE IF NOT EXISTS persons (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name   TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS cases (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    case_number     TEXT NOT NULL UNIQUE,
    incident_date   TEXT NOT NULL,
    officer_id      INTEGER NOT NULL REFERENCES officers(id)
);

CREATE TABLE IF NOT EXISTS case_persons (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    case_id     INTEGER NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    person_id   INTEGER NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    role        TEXT NOT NULL,
    UNIQUE(case_id, person_id)
);

CREATE TABLE IF NOT EXISTS charges (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    code        TEXT NOT NULL UNIQUE,
    description TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS case_charges (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    case_id     INTEGER NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    charge_id   INTEGER NOT NULL REFERENCES charges(id),
    UNIQUE(case_id, charge_id)
);

CREATE TABLE IF NOT EXISTS ammunition_calibers (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    name    TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS electronic_types (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    name    TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS narcotics_types (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    name    TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS evidence (
    id                          INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode                     TEXT NOT NULL UNIQUE,
    case_id                     INTEGER NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    collected_by_officer_id     INTEGER REFERENCES officers(id),
    collected_from_person_id    INTEGER REFERENCES persons(id),
    collection_date             TEXT,
    specific_location           TEXT,
    address                     TEXT,
    city                        TEXT,
    state                       TEXT,
    zip                         TEXT,
    evidence_type               TEXT NOT NULL,
    description                 TEXT NOT NULL,
    status                      TEXT NOT NULL DEFAULT 'Active',
    storage_location            TEXT,
    -- Ammunition
    ammo_caliber                TEXT,
    ammo_quantity               TEXT,
    ammo_grain_weight           TEXT,
    ammo_bullet_type            TEXT,
    ammo_brand                  TEXT,
    -- Biological / DNA
    bio_sample_type             TEXT,
    bio_collection_method       TEXT,
    bio_storage_temp            TEXT,
    bio_suspect_name            TEXT,
    bio_dna_analysis_requested  INTEGER DEFAULT 0,
    -- Currency
    currency_amount             TEXT,
    currency_denominations      TEXT,
    currency_serial_numbers     TEXT,
    currency_suspected_counterfeit INTEGER DEFAULT 0,
    -- Electronics
    elec_device_type            TEXT,
    elec_make                   TEXT,
    elec_model                  TEXT,
    elec_serial_number          TEXT,
    elec_password_protected     INTEGER DEFAULT 0,
    elec_data_extraction_requested INTEGER DEFAULT 0,
    -- Firearm
    firearm_make                TEXT,
    firearm_model               TEXT,
    firearm_serial_number       TEXT,
    firearm_type                TEXT,
    firearm_caliber             TEXT,
    firearm_reported_stolen     INTEGER DEFAULT 0,
    firearm_loaded_when_recovered INTEGER DEFAULT 0,
    -- Jewelry
    jewelry_type                TEXT,
    jewelry_material            TEXT,
    jewelry_estimated_value     TEXT,
    jewelry_engraving_or_id     TEXT,
    -- Narcotic Equipment
    narc_equip_type             TEXT,
    narc_equip_description      TEXT,
    narc_equip_suspected_residue TEXT,
    narc_equip_field_test_kit_used INTEGER DEFAULT 0,
    -- Narcotics
    narc_drug_type              TEXT,
    narc_net_weight             TEXT,
    narc_form                   TEXT,
    narc_packaging              TEXT,
    narc_field_test_performed   INTEGER DEFAULT 0,
    narc_field_test_result      TEXT,
    -- Vehicle
    vehicle_make                TEXT,
    vehicle_model               TEXT,
    vehicle_year                TEXT,
    vehicle_color               TEXT,
    vehicle_vin                 TEXT,
    vehicle_license_plate       TEXT,
    vehicle_license_state       TEXT,
    vehicle_reported_stolen     INTEGER DEFAULT 0,
    vehicle_impounded           INTEGER DEFAULT 0,
    -- Weapon
    weapon_type                 TEXT,
    weapon_make                 TEXT,
    weapon_model                TEXT,
    weapon_serial_number        TEXT,
    weapon_length               TEXT,
    weapon_reported_stolen      INTEGER DEFAULT 0
);
