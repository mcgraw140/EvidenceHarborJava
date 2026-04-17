-- Seed data

INSERT OR IGNORE INTO officers (name, badge) VALUES ('Casey Mcgraw', 'B001');
INSERT OR IGNORE INTO officers (name, badge) VALUES ('John Smith', 'B002');

INSERT OR IGNORE INTO charges (code, description) VALUES
    ('13A-12-213', 'Unlawful Possession of Marijuana (1st Degree)'),
    ('13A-12-260', 'Possession of Drug paraphernalia (No Degree)'),
    ('13A-8-4',    'Theft of Property (1st Degree)'),
    ('13A-11-7',   'Disorderly Conduct');

INSERT OR IGNORE INTO weapon_types (name) VALUES
    ('Knife'), ('Sword'), ('Machete'), ('Bat/Club'), ('Taser/Stun Gun'),
    ('Brass Knuckles'), ('Bow'), ('Spear'), ('Other');

INSERT OR IGNORE INTO biological_sources (name) VALUES
    ('Blood Swab'), ('Saliva Swab'), ('Hair Sample'), ('Tissue Sample'),
    ('Urine Sample'), ('Fingernail Clipping'), ('Buccal Swab'), ('Other');

INSERT OR IGNORE INTO narcotics_unit_types (name) VALUES
    ('Grams'), ('Ounces'), ('Pounds'), ('Kilograms'),
    ('Pills / Tablets'), ('Capsules'), ('Milliliters'), ('Units');

INSERT OR IGNORE INTO person_roles (name) VALUES
    ('Victim'), ('Suspect'), ('Witness'), ('Owner'), ('Complainant'),
    ('Informant'), ('Co-Defendant'), ('Other');

INSERT OR IGNORE INTO transfer_actions (name) VALUES
    ('Check In'), ('Check Out to Person'), ('Submit for Analysis'),
    ('Agency Transfer'), ('Bank Deposit'), ('Bank Removal'),
    ('Destroy'), ('Disburse'), ('Move to New Location'), ('Return to Owner');

INSERT OR IGNORE INTO analysis_labs (name) VALUES
    ('State Forensic Lab'), ('FBI Regional Lab'), ('Local Crime Lab'),
    ('Toxicology Lab'), ('DNA Analysis Lab'), ('Ballistics Lab'), ('Other');

INSERT OR IGNORE INTO other_agencies (name) VALUES
    ('FBI'), ('DEA'), ('ATF'), ('State Police'), ('County Sheriff'), ('Other');

INSERT OR IGNORE INTO case_statuses (name) VALUES
    ('Open'), ('Closed'), ('Pending Review'), ('Archived'), ('Inactive');

INSERT OR IGNORE INTO evidence_storage_locations (name) VALUES
    ('Main Evidence Room'), ('Freezer'), ('Secure Locker'), ('Vault'),
    ('Narcotics Safe'), ('Firearm Locker'), ('Vehicle Impound Lot'), ('Offsite Storage');

INSERT OR IGNORE INTO intake_locations (name) VALUES
    ('Front Desk'), ('Evidence Drop Box'), ('Field Collection'), ('Lab Submission'), ('Other');

INSERT OR IGNORE INTO ammunition_calibers (name) VALUES
    ('9mm'), ('.45 ACP'), ('.40 S&W'), ('5.56 NATO'), ('.380 ACP'), ('.357 Magnum'), ('12 Gauge');

INSERT OR IGNORE INTO electronic_types (name) VALUES
    ('Cell Phone'), ('Laptop'), ('Tablet'), ('Desktop Computer'), ('USB Drive'), ('GPS Device'), ('Camera');

INSERT OR IGNORE INTO narcotics_types (name) VALUES
    ('Marijuana'), ('Cocaine'), ('Methamphetamine'), ('Heroin'), ('Fentanyl'), ('MDMA'), ('Prescription Pills');

INSERT INTO persons (full_name)
SELECT 'Unknown Person'
WHERE NOT EXISTS (SELECT 1 FROM persons WHERE full_name='Unknown Person');
INSERT INTO persons (full_name)
SELECT 'John Johnson'
WHERE NOT EXISTS (SELECT 1 FROM persons WHERE full_name='John Johnson');
INSERT INTO persons (full_name)
SELECT 'Jane Doe'
WHERE NOT EXISTS (SELECT 1 FROM persons WHERE full_name='Jane Doe');
INSERT INTO persons (full_name)
SELECT 'BEST PERSON EVER'
WHERE NOT EXISTS (SELECT 1 FROM persons WHERE full_name='BEST PERSON EVER');

INSERT OR IGNORE INTO cases (case_number, incident_date, officer_id)
VALUES ('20250401-0001', '2026-01-28', (SELECT id FROM officers WHERE name='Casey Mcgraw'));
INSERT OR IGNORE INTO cases (case_number, incident_date, officer_id)
VALUES ('25-00002', '2026-01-27', (SELECT id FROM officers WHERE name='Casey Mcgraw'));
INSERT OR IGNORE INTO cases (case_number, incident_date, officer_id)
VALUES ('2025-0002', '2026-01-27', (SELECT id FROM officers WHERE name='Casey Mcgraw'));
INSERT OR IGNORE INTO cases (case_number, incident_date, officer_id)
VALUES ('CB-2025-0001', '2026-01-26', (SELECT id FROM officers WHERE name='Casey Mcgraw'));
INSERT OR IGNORE INTO cases (case_number, incident_date, officer_id)
VALUES ('25-00001', '2026-01-26', (SELECT id FROM officers WHERE name='Casey Mcgraw'));

INSERT OR IGNORE INTO case_persons (case_id, person_id, role) VALUES (
    (SELECT id FROM cases WHERE case_number='20250401-0001'),
    (SELECT id FROM persons WHERE full_name='Unknown Person'), 'Victim');
INSERT OR IGNORE INTO case_persons (case_id, person_id, role) VALUES (
    (SELECT id FROM cases WHERE case_number='20250401-0001'),
    (SELECT id FROM persons WHERE full_name='BEST PERSON EVER'), 'Owner');

INSERT OR IGNORE INTO case_charges (case_id, charge_id) VALUES (
    (SELECT id FROM cases WHERE case_number='20250401-0001'),
    (SELECT id FROM charges WHERE code='13A-12-213'));
INSERT OR IGNORE INTO case_charges (case_id, charge_id) VALUES (
    (SELECT id FROM cases WHERE case_number='20250401-0001'),
    (SELECT id FROM charges WHERE code='13A-12-260'));

INSERT OR IGNORE INTO case_persons (case_id, person_id, role) VALUES (
    (SELECT id FROM cases WHERE case_number='25-00002'),
    (SELECT id FROM persons WHERE full_name='John Johnson'), 'Victim');
INSERT OR IGNORE INTO case_persons (case_id, person_id, role) VALUES (
    (SELECT id FROM cases WHERE case_number='2025-0002'),
    (SELECT id FROM persons WHERE full_name='John Johnson'), 'Suspect');

INSERT OR IGNORE INTO evidence (
    barcode, case_id, collected_by_officer_id, collection_date,
    evidence_type, description, status, storage_location, currency_amount
) VALUES (
    '2026-CURRENCY-0004',
    (SELECT id FROM cases WHERE case_number='20250401-0001'),
    (SELECT id FROM officers WHERE name='Casey Mcgraw'),
    '2026-01-28',
    'Currency', '2500Currency all 100''s', 'In Storage', 'Bank Deposit', '2500.00');
