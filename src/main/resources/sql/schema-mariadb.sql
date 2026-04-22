-- Evidence Harbor MariaDB Schema

CREATE TABLE IF NOT EXISTS agency_settings (
    k VARCHAR(100) PRIMARY KEY,
    v TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS officers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    badge VARCHAR(100),
    username VARCHAR(100) UNIQUE,
    password_hash TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'officer',
    status VARCHAR(50) NOT NULL DEFAULT 'Active',
    is_external TINYINT(1) NOT NULL DEFAULT 0,
    permissions TEXT
);

CREATE TABLE IF NOT EXISTS persons (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    dob VARCHAR(20),
    ssn VARCHAR(20),
    street VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(10),
    zip VARCHAR(20),
    contact VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS cases (
    id INT AUTO_INCREMENT PRIMARY KEY,
    case_number VARCHAR(255) NOT NULL UNIQUE,
    incident_date VARCHAR(50) NOT NULL,
    officer_id INT NOT NULL,
    FOREIGN KEY (officer_id) REFERENCES officers(id)
);

CREATE TABLE IF NOT EXISTS case_persons (
    id INT AUTO_INCREMENT PRIMARY KEY,
    case_id INT NOT NULL,
    person_id INT NOT NULL,
    role VARCHAR(100) NOT NULL,
    UNIQUE KEY uq_case_persons (case_id, person_id),
    FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE,
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS charges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS case_charges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    case_id INT NOT NULL,
    charge_id INT NOT NULL,
    UNIQUE KEY uq_case_charges (case_id, charge_id),
    FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE,
    FOREIGN KEY (charge_id) REFERENCES charges(id)
);

CREATE TABLE IF NOT EXISTS ammunition_calibers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS electronic_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS narcotics_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS evidence (
    id INT AUTO_INCREMENT PRIMARY KEY,
    barcode VARCHAR(255) NOT NULL UNIQUE,
    case_id INT NOT NULL,
    collected_by_officer_id INT,
    collected_from_person_id INT,
    collection_date VARCHAR(50),
    specific_location TEXT,
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(50),
    zip VARCHAR(20),
    evidence_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(100) NOT NULL DEFAULT 'In Custody',
    storage_location VARCHAR(255),
    ammo_caliber VARCHAR(100),
    ammo_quantity VARCHAR(100),
    ammo_grain_weight VARCHAR(100),
    ammo_bullet_type VARCHAR(100),
    ammo_brand VARCHAR(100),
    bio_sample_type VARCHAR(100),
    bio_collection_method VARCHAR(100),
    bio_storage_temp VARCHAR(100),
    bio_suspect_name VARCHAR(255),
    bio_dna_analysis_requested TINYINT(1) DEFAULT 0,
    currency_amount VARCHAR(100),
    currency_denominations TEXT,
    currency_serial_numbers TEXT,
    currency_suspected_counterfeit TINYINT(1) DEFAULT 0,
    elec_device_type VARCHAR(100),
    elec_make VARCHAR(100),
    elec_model VARCHAR(100),
    elec_serial_number VARCHAR(100),
    elec_password_protected TINYINT(1) DEFAULT 0,
    elec_data_extraction_requested TINYINT(1) DEFAULT 0,
    elec_device_username VARCHAR(255),
    elec_device_password TEXT,
    firearm_make VARCHAR(100),
    firearm_model VARCHAR(100),
    firearm_serial_number VARCHAR(100),
    firearm_type VARCHAR(100),
    firearm_caliber VARCHAR(100),
    firearm_reported_stolen TINYINT(1) DEFAULT 0,
    firearm_loaded_when_recovered TINYINT(1) DEFAULT 0,
    jewelry_type VARCHAR(100),
    jewelry_material VARCHAR(100),
    jewelry_estimated_value VARCHAR(100),
    jewelry_engraving_or_id VARCHAR(255),
    narc_equip_type VARCHAR(100),
    narc_equip_description TEXT,
    narc_equip_suspected_residue VARCHAR(255),
    narc_equip_field_test_kit_used TINYINT(1) DEFAULT 0,
    narc_drug_type VARCHAR(100),
    narc_net_weight VARCHAR(100),
    narc_form VARCHAR(100),
    narc_packaging VARCHAR(255),
    narc_field_test_performed TINYINT(1) DEFAULT 0,
    narc_field_test_result VARCHAR(255),
    vehicle_body_type VARCHAR(100),
    vehicle_make VARCHAR(100),
    vehicle_model VARCHAR(100),
    vehicle_year VARCHAR(20),
    vehicle_color VARCHAR(100),
    vehicle_vin VARCHAR(100),
    vehicle_license_plate VARCHAR(100),
    vehicle_license_state VARCHAR(50),
    vehicle_reported_stolen TINYINT(1) DEFAULT 0,
    vehicle_impounded TINYINT(1) DEFAULT 0,
    weapon_type VARCHAR(100),
    weapon_make VARCHAR(100),
    weapon_model VARCHAR(100),
    weapon_serial_number VARCHAR(100),
    weapon_length VARCHAR(100),
    weapon_reported_stolen TINYINT(1) DEFAULT 0,
    FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE,
    FOREIGN KEY (collected_by_officer_id) REFERENCES officers(id),
    FOREIGN KEY (collected_from_person_id) REFERENCES persons(id)
);

CREATE TABLE IF NOT EXISTS weapon_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS vehicle_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS impound_locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS biological_sources (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS narcotics_unit_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS person_roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS transfer_actions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS analysis_labs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS other_agencies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS case_statuses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS evidence_storage_locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS intake_locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS evidence_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS evidence_statuses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS audit_modules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS audit_actions_lookup (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS audit_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS user_roles_lookup (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS user_statuses_lookup (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS chain_of_custody (
    id INT AUTO_INCREMENT PRIMARY KEY,
    evidence_id INT NOT NULL,
    action VARCHAR(100) NOT NULL,
    performed_by VARCHAR(255),
    performed_by_name VARCHAR(255),
    from_location VARCHAR(255),
    to_location VARCHAR(255),
    to_person VARCHAR(255),
    reason TEXT,
    notes TEXT,
    signature_data LONGTEXT,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (evidence_id) REFERENCES evidence(id)
);

CREATE TABLE IF NOT EXISTS dropbox_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    officer_name VARCHAR(255),
    item_count INT DEFAULT 0,
    items_json LONGTEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_name VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    module VARCHAR(100),
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    details LONGTEXT
);

CREATE TABLE IF NOT EXISTS evidence_audits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    audit_type VARCHAR(100) NOT NULL,
    scope VARCHAR(255),
    created_by VARCHAR(255),
    status VARCHAR(100) NOT NULL DEFAULT 'In Progress',
    items_json LONGTEXT
);

CREATE TABLE IF NOT EXISTS bank_accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_name VARCHAR(255) NOT NULL,
    account_number VARCHAR(100),
    bank_name VARCHAR(255),
    balance DECIMAL(12,2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bank_account_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    action VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    slip_number VARCHAR(100),
    date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    performed_by VARCHAR(255),
    notes TEXT,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_reason TEXT,
    voided_by VARCHAR(255),
    voided_at DATETIME,
    source_ref VARCHAR(255),
    FOREIGN KEY (account_id) REFERENCES bank_accounts(id)
);

