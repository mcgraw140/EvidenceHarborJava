-- Seed data

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

INSERT OR IGNORE INTO evidence_types (name) VALUES
    ('Ammunition'), ('Biological / DNA'), ('Currency'), ('Electronics'),
    ('Firearm'), ('Jewelry'), ('Narcotic Equipment'), ('Narcotics'), ('Weapon');

INSERT OR IGNORE INTO evidence_statuses (name) VALUES
    ('In Dropbox'), ('In Custody'), ('In Storage'), ('Checked In'), ('Checked Out'),
    ('Deposited'), ('Missing'), ('Destroyed'), ('Disbursed'), ('Returned to Owner'), ('Pending');

INSERT OR IGNORE INTO audit_modules (name) VALUES
    ('Evidence'), ('Cases'), ('Users'), ('Narcotics'), ('Quartermaster'), ('System');

INSERT OR IGNORE INTO audit_actions_lookup (name) VALUES
    ('CREATE'), ('UPDATE'), ('DELETE'), ('PRINT'), ('SAVE'), ('LOGIN'), ('LOGOUT');

INSERT OR IGNORE INTO audit_types (name) VALUES
    ('Full'), ('Random'), ('Location');

INSERT OR IGNORE INTO user_roles_lookup (name) VALUES
    ('officer'), ('supervisor'), ('agency_admin');

INSERT OR IGNORE INTO user_statuses_lookup (name) VALUES
    ('Active'), ('Inactive');

INSERT OR IGNORE INTO qm_equipment_categories_lookup (name) VALUES
    ('Weapon'), ('Uniform'), ('Equipment'), ('Vehicle'), ('Other');

INSERT OR IGNORE INTO qm_equipment_statuses_lookup (name) VALUES
    ('Available'), ('Assigned'), ('Maintenance');

INSERT OR IGNORE INTO qm_vehicle_statuses_lookup (name) VALUES
    ('Impounded'), ('Released');

INSERT OR IGNORE INTO qm_storage_locations (name) VALUES
    ('QM Main Cage'), ('QM Armory'), ('QM Uniform Room'), ('QM Ammo Locker'), ('QM Vehicle Bay');
