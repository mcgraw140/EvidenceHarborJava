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
    -- CPD Rack A
    ('CPD A-1-A'), ('CPD A-1-B'), ('CPD A-2-A'), ('CPD A-2-B'), ('CPD A-3-A'), ('CPD A-3-B'), ('CPD A-4-A'), ('CPD A-4-B'), ('CPD A-5-A'), ('CPD A-5-B'),
    -- CPD Rack B
    ('CPD B-1-A'), ('CPD B-1-B'), ('CPD B-2-A'), ('CPD B-2-B'), ('CPD B-3-A'), ('CPD B-3-B'), ('CPD B-4-A'), ('CPD B-4-B'), ('CPD B-5-A'), ('CPD B-5-B'),
    -- CPD Rack C
    ('CPD C-1-A'), ('CPD C-1-B'), ('CPD C-2-A'), ('CPD C-2-B'), ('CPD C-3-A'), ('CPD C-3-B'), ('CPD C-4-A'), ('CPD C-4-B'), ('CPD C-5-A'), ('CPD C-5-B'),
    -- CPD Rack D
    ('CPD D-1-A'), ('CPD D-1-B'), ('CPD D-2-A'), ('CPD D-2-B'), ('CPD D-3-A'), ('CPD D-3-B'), ('CPD D-4-A'), ('CPD D-4-B'), ('CPD D-5-A'), ('CPD D-5-B'),
    -- CPD Rack E
    ('CPD E-1-A'), ('CPD E-1-B'), ('CPD E-2-A'), ('CPD E-2-B'), ('CPD E-3-A'), ('CPD E-3-B'), ('CPD E-4-A'), ('CPD E-4-B'), ('CPD E-5-A'), ('CPD E-5-B'),
    -- CPD Rack F
    ('CPD F-1-A'), ('CPD F-1-B'), ('CPD F-2-A'), ('CPD F-2-B'), ('CPD F-3-A'), ('CPD F-3-B'), ('CPD F-4-A'), ('CPD F-4-B'), ('CPD F-5-A'), ('CPD F-5-B'),
    -- CPD Rack G
    ('CPD G-1-A'), ('CPD G-1-B'), ('CPD G-2-A'), ('CPD G-2-B'), ('CPD G-3-A'), ('CPD G-3-B'), ('CPD G-4-A'), ('CPD G-4-B'), ('CPD G-5-A'), ('CPD G-5-B'),
    -- CPD Rack H
    ('CPD H-1-A'), ('CPD H-1-B'), ('CPD H-2-A'), ('CPD H-2-B'), ('CPD H-3-A'), ('CPD H-3-B'), ('CPD H-4-A'), ('CPD H-4-B'), ('CPD H-5-A'), ('CPD H-5-B'),
    -- ARMORY Rack A
    ('ARMORY A-1-A'), ('ARMORY A-1-B'), ('ARMORY A-2-A'), ('ARMORY A-2-B'), ('ARMORY A-3-A'), ('ARMORY A-3-B'), ('ARMORY A-4-A'), ('ARMORY A-4-B'), ('ARMORY A-5-A'), ('ARMORY A-5-B'),
    -- ARMORY Rack B
    ('ARMORY B-1-A'), ('ARMORY B-1-B'), ('ARMORY B-2-A'), ('ARMORY B-2-B'), ('ARMORY B-3-A'), ('ARMORY B-3-B'), ('ARMORY B-4-A'), ('ARMORY B-4-B'), ('ARMORY B-5-A'), ('ARMORY B-5-B'),
    -- ARMORY Rack C
    ('ARMORY C-1-A'), ('ARMORY C-1-B'), ('ARMORY C-2-A'), ('ARMORY C-2-B'), ('ARMORY C-3-A'), ('ARMORY C-3-B'), ('ARMORY C-4-A'), ('ARMORY C-4-B'), ('ARMORY C-5-A'), ('ARMORY C-5-B'),
    -- ARMORY Rack D
    ('ARMORY D-1-A'), ('ARMORY D-1-B'), ('ARMORY D-2-A'), ('ARMORY D-2-B'), ('ARMORY D-3-A'), ('ARMORY D-3-B'), ('ARMORY D-4-A'), ('ARMORY D-4-B'), ('ARMORY D-5-A'), ('ARMORY D-5-B'),
    -- ARMORY Rack E
    ('ARMORY E-1-A'), ('ARMORY E-1-B'), ('ARMORY E-2-A'), ('ARMORY E-2-B'), ('ARMORY E-3-A'), ('ARMORY E-3-B'), ('ARMORY E-4-A'), ('ARMORY E-4-B'), ('ARMORY E-5-A'), ('ARMORY E-5-B'),
    -- ARMORY Rack F
    ('ARMORY F-1-A'), ('ARMORY F-1-B'), ('ARMORY F-2-A'), ('ARMORY F-2-B'), ('ARMORY F-3-A'), ('ARMORY F-3-B'), ('ARMORY F-4-A'), ('ARMORY F-4-B'), ('ARMORY F-5-A'), ('ARMORY F-5-B'),
    -- ARMORY Rack G
    ('ARMORY G-1-A'), ('ARMORY G-1-B'), ('ARMORY G-2-A'), ('ARMORY G-2-B'), ('ARMORY G-3-A'), ('ARMORY G-3-B'), ('ARMORY G-4-A'), ('ARMORY G-4-B'), ('ARMORY G-5-A'), ('ARMORY G-5-B'),
    -- ARMORY Rack H
    ('ARMORY H-1-A'), ('ARMORY H-1-B'), ('ARMORY H-2-A'), ('ARMORY H-2-B'), ('ARMORY H-3-A'), ('ARMORY H-3-B'), ('ARMORY H-4-A'), ('ARMORY H-4-B'), ('ARMORY H-5-A'), ('ARMORY H-5-B');

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