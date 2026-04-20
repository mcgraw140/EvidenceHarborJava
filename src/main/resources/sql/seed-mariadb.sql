-- Seed data for MariaDB

INSERT IGNORE INTO charges (code, description) VALUES
    ('13A-12-213', 'Unlawful Possession of Marijuana (1st Degree)'),
    ('13A-12-260', 'Possession of Drug paraphernalia (No Degree)'),
    ('13A-8-4',    'Theft of Property (1st Degree)'),
    ('13A-11-7',   'Disorderly Conduct');

INSERT IGNORE INTO weapon_types (name) VALUES
    ('Knife'), ('Sword'), ('Machete'), ('Bat/Club'), ('Taser/Stun Gun'),
    ('Brass Knuckles'), ('Bow'), ('Spear'), ('Other');

INSERT IGNORE INTO biological_sources (name) VALUES
    ('Blood Swab'), ('Saliva Swab'), ('Hair Sample'), ('Tissue Sample'),
    ('Urine Sample'), ('Fingernail Clipping'), ('Buccal Swab'), ('Other');

INSERT IGNORE INTO narcotics_unit_types (name) VALUES
    ('Grams'), ('Ounces'), ('Pounds'), ('Kilograms'),
    ('Pills / Tablets'), ('Capsules'), ('Milliliters'), ('Units');

INSERT IGNORE INTO person_roles (name) VALUES
    ('Victim'), ('Suspect'), ('Witness'), ('Owner'), ('Complainant'),
    ('Informant'), ('Co-Defendant'), ('Other');

INSERT IGNORE INTO transfer_actions (name) VALUES
    ('Check In'), ('Check Out to Person'), ('Submit for Analysis'),
    ('Agency Transfer'), ('Bank Deposit'), ('Bank Removal'),
    ('Destroy'), ('Disburse'), ('Move to New Location'), ('Return to Owner');

INSERT IGNORE INTO analysis_labs (name) VALUES
    ('State Forensic Lab'), ('FBI Regional Lab'), ('Local Crime Lab'),
    ('Toxicology Lab'), ('DNA Analysis Lab'), ('Ballistics Lab'), ('Other');

INSERT IGNORE INTO other_agencies (name) VALUES
    ('FBI'), ('DEA'), ('ATF'), ('State Police'), ('County Sheriff'), ('Other');

INSERT IGNORE INTO case_statuses (name) VALUES
    ('Open'), ('Closed'), ('Pending Review'), ('Archived'), ('Inactive');

INSERT IGNORE INTO evidence_storage_locations (name) VALUES
    ('Main Evidence Room'), ('Freezer'), ('Secure Locker'), ('Vault'),
    ('Narcotics Safe'), ('Firearm Locker'), ('Vehicle Impound Lot'), ('Offsite Storage');

INSERT IGNORE INTO intake_locations (name) VALUES
    ('Front Desk'), ('Evidence Drop Box'), ('Field Collection'), ('Lab Submission'), ('Other');

INSERT IGNORE INTO ammunition_calibers (name) VALUES
    ('9mm'), ('.45 ACP'), ('.40 S&W'), ('5.56 NATO'), ('.380 ACP'), ('.357 Magnum'), ('12 Gauge');

INSERT IGNORE INTO electronic_types (name) VALUES
    ('Cell Phone'), ('Laptop'), ('Tablet'), ('Desktop Computer'), ('USB Drive'), ('GPS Device'), ('Camera');

INSERT IGNORE INTO narcotics_types (name) VALUES
    ('Marijuana'), ('Cocaine'), ('Methamphetamine'), ('Heroin'), ('Fentanyl'), ('MDMA'), ('Prescription Pills');

INSERT IGNORE INTO evidence_types (name) VALUES
    ('Ammunition'), ('Biological / DNA'), ('Currency'), ('Electronics'),
    ('Firearm'), ('Jewelry'), ('Narcotic Equipment'), ('Narcotics'), ('Weapon');

INSERT IGNORE INTO evidence_statuses (name) VALUES
    ('In Dropbox'), ('In Custody'), ('In Storage'), ('Checked In'), ('Checked Out'),
    ('Deposited'), ('Missing'), ('Destroyed'), ('Disbursed'), ('Returned to Owner'), ('Pending');

INSERT IGNORE INTO audit_modules (name) VALUES
    ('Evidence'), ('Cases'), ('Users'), ('Narcotics'), ('System');

INSERT IGNORE INTO audit_actions_lookup (name) VALUES
    ('CREATE'), ('UPDATE'), ('DELETE'), ('PRINT'), ('SAVE'), ('LOGIN'), ('LOGOUT');

INSERT IGNORE INTO audit_types (name) VALUES
    ('Full'), ('Random'), ('Location');

INSERT IGNORE INTO user_roles_lookup (name) VALUES
    ('officer'), ('evidence_tech'), ('admin');

INSERT IGNORE INTO user_statuses_lookup (name) VALUES
    ('Active'), ('Inactive');