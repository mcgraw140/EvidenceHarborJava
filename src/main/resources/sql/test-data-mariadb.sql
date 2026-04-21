-- =============================================================================
-- Evidence Harbor - Test Data
-- Run this manually in MariaDB AFTER the app has been started at least once
-- (so the schema and seed are applied first).
--
-- PowerShell usage:
--   Get-Content "src/main/resources/sql/test-data-mariadb.sql" | & "C:\Program Files\MariaDB 12.2\bin\mysql.exe" -u root -p evidence_harbor
-- =============================================================================

-- Store officer IDs in variables from the first 3 officers available
SET @officer1 = (SELECT id FROM officers ORDER BY id LIMIT 1);
SET @officer2 = (SELECT id FROM officers ORDER BY id LIMIT 1 OFFSET 1);
SET @officer3 = (SELECT id FROM officers ORDER BY id LIMIT 1 OFFSET 2);
-- If fewer than 3 officers exist, fall back to officer1
SET @officer2 = COALESCE(@officer2, @officer1);
SET @officer3 = COALESCE(@officer3, @officer1);

-- -------------------------
-- Insert 15 test cases
-- -------------------------
INSERT IGNORE INTO cases (case_number, incident_date, officer_id) VALUES
    ('2025-001001', '2025-01-05', @officer1),
    ('2025-001002', '2025-01-12', @officer2),
    ('2025-001003', '2025-01-20', @officer1),
    ('2025-002001', '2025-02-03', @officer1),
    ('2025-002002', '2025-02-14', @officer2),
    ('2025-002003', '2025-02-22', @officer1),
    ('2025-003001', '2025-03-08', @officer1),
    ('2025-003002', '2025-03-15', @officer2),
    ('2025-003003', '2025-03-27', @officer1),
    ('2025-004001', '2025-04-02', @officer2),
    ('2025-004002', '2025-04-11', @officer2),
    ('2025-004003', '2025-04-19', @officer1),
    ('2025-005001', '2025-05-06', @officer1),
    ('2025-005002', '2025-05-18', @officer2),
    ('2025-005003', '2025-05-29', @officer1);

-- Store the IDs we just created
SET @c01 = (SELECT id FROM cases WHERE case_number = '2025-001001');
SET @c02 = (SELECT id FROM cases WHERE case_number = '2025-001002');
SET @c03 = (SELECT id FROM cases WHERE case_number = '2025-001003');
SET @c04 = (SELECT id FROM cases WHERE case_number = '2025-002001');
SET @c05 = (SELECT id FROM cases WHERE case_number = '2025-002002');
SET @c06 = (SELECT id FROM cases WHERE case_number = '2025-002003');
SET @c07 = (SELECT id FROM cases WHERE case_number = '2025-003001');
SET @c08 = (SELECT id FROM cases WHERE case_number = '2025-003002');
SET @c09 = (SELECT id FROM cases WHERE case_number = '2025-003003');
SET @c10 = (SELECT id FROM cases WHERE case_number = '2025-004001');
SET @c11 = (SELECT id FROM cases WHERE case_number = '2025-004002');
SET @c12 = (SELECT id FROM cases WHERE case_number = '2025-004003');
SET @c13 = (SELECT id FROM cases WHERE case_number = '2025-005001');
SET @c14 = (SELECT id FROM cases WHERE case_number = '2025-005002');
SET @c15 = (SELECT id FROM cases WHERE case_number = '2025-005003');

-- -------------------------
-- Insert evidence (2-4 items per case, varied types/locations/statuses)
-- -------------------------

-- Case 2025-001001 (Narcotics + Firearm) — items just dropped off, still in intake
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-NARC-0001', @c01, @officer1, '2025-01-05', 'Narcotics', 'Suspected marijuana, approx 28g in ziplock bag', 'In Dropbox', 'Evidence Drop Box'),
    ('2025-FIRE-0001', @c01, @officer1, '2025-01-05', 'Firearm', 'Glock 19, 9mm, S/N: GLK192025A, loaded', 'In Dropbox', 'Weapons Locker 1'),
    ('2025-NARC-0002', @c01, @officer1, '2025-01-05', 'Narcotic Equipment', 'Glass pipe with suspected residue', 'In Dropbox', 'Evidence Drop Box');

-- Case 2025-001002 (Electronics + Currency) — processed into storage
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-ELEC-0001', @c02, @officer2, '2025-01-12', 'Electronics', 'Samsung Galaxy S24, black, IMEI: 352112001234567', 'In Storage', 'CPD B-1-A'),
    ('2025-CURR-0001', @c02, @officer2, '2025-01-12', 'Currency', 'Cash - $1,240.00 mixed denominations', 'Deposited', NULL),
    ('2025-ELEC-0002', @c02, @officer2, '2025-01-12', 'Electronics', 'USB drive, 64GB, black SanDisk', 'In Storage', 'CPD B-1-B');

-- Case 2025-001003 (Weapon + Biological) — processed into storage
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-WEAP-0001', @c03, @officer1, '2025-01-20', 'Weapon', 'Fixed-blade knife, 6-inch blade, black handle', 'In Storage', 'CPD C-2-A'),
    ('2025-BIO-0001',  @c03, @officer1, '2025-01-20', 'Biological / DNA', 'Blood swab collected from door handle, Item A', 'In Storage', 'CPD C-2-B'),
    ('2025-BIO-0002',  @c03, @officer1, '2025-01-20', 'Biological / DNA', 'Blood swab collected from victim clothing, Item B', 'In Storage', 'CPD C-2-C');

-- Case 2025-002001 (Firearm + Ammunition) — still in intake
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-FIRE-0002', @c04, @officer1, '2025-02-03', 'Firearm', 'Smith & Wesson M&P Shield .40 S/N: SWS402025B, unloaded', 'In Dropbox', 'Weapons Locker 2'),
    ('2025-AMMO-0001', @c04, @officer1, '2025-02-03', 'Ammunition', '.40 S&W, 15 rounds, loose in bag', 'In Dropbox', 'Weapons Locker 2'),
    ('2025-AMMO-0002', @c04, @officer1, '2025-02-03', 'Ammunition', '.40 S&W, full 15-round magazine', 'In Dropbox', 'Weapons Locker 2');

-- Case 2025-002002 (Narcotics + Currency + Electronics) — mixed statuses
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-NARC-0003', @c05, @officer2, '2025-02-14', 'Narcotics', 'Suspected cocaine, approx 14g, white powder in clear bag', 'In Storage', 'CPD D-3-A'),
    ('2025-CURR-0002', @c05, @officer2, '2025-02-14', 'Currency', 'Cash - $480.00 in small bills', 'Deposited', NULL),
    ('2025-ELEC-0003', @c05, @officer2, '2025-02-14', 'Electronics', 'Apple iPhone 15, cracked screen, black case', 'Checked Out', 'CPD D-3-B'),
    ('2025-NARC-0004', @c05, @officer2, '2025-02-14', 'Narcotic Equipment', 'Digital scale, 0-500g capacity', 'In Storage', 'CPD D-3-C');

-- Case 2025-002003 (Jewelry + Currency) — processed
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-JEWL-0001', @c06, @officer1, '2025-02-22', 'Jewelry', 'Gold ring with diamond setting, engraved "J&M 2019"', 'In Storage', 'CPD E-1-A'),
    ('2025-JEWL-0002', @c06, @officer1, '2025-02-22', 'Jewelry', 'Silver necklace with pendant, approx 18 inches', 'In Storage', 'CPD E-1-B'),
    ('2025-CURR-0003', @c06, @officer1, '2025-02-22', 'Currency', 'Cash - $2,750.00 assorted bills', 'Deposited', NULL);

-- Case 2025-003001 (Electronics + Weapon) — in storage
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-ELEC-0004', @c07, @officer1, '2025-03-08', 'Electronics', 'Dell laptop, silver, S/N: DL20250308X', 'In Storage', 'CPD F-2-A'),
    ('2025-WEAP-0002', @c07, @officer1, '2025-03-08', 'Weapon', 'Baseball bat, wooden, dried blood present', 'In Storage', 'CPD F-2-B');

-- Case 2025-003002 (Narcotics) — mixed: two in storage, one still in dropbox
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-NARC-0005', @c08, @officer2, '2025-03-15', 'Narcotics', 'Suspected methamphetamine, 7g, crystalline substance', 'In Storage', 'CPD G-1-A'),
    ('2025-NARC-0006', @c08, @officer2, '2025-03-15', 'Narcotics', 'Suspected fentanyl pills, 32 blue tablets', 'In Storage', 'CPD G-1-B'),
    ('2025-NARC-0007', @c08, @officer2, '2025-03-15', 'Narcotic Equipment', 'Syringe (used), biohazard bag', 'In Dropbox', 'Evidence Drop Box');

-- Case 2025-003003 (Firearm + Biological + Ammunition) — in storage
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-FIRE-0003', @c09, @officer1, '2025-03-27', 'Firearm', 'Ruger LCP .380, S/N: RUG3802025C, unloaded', 'In Storage', 'ARMORY A-1-A'),
    ('2025-BIO-0003',  @c09, @officer1, '2025-03-27', 'Biological / DNA', 'Buccal swab - suspect sample, sealed envelope', 'In Storage', 'CPD H-1-A'),
    ('2025-AMMO-0003', @c09, @officer1, '2025-03-27', 'Ammunition', '.380 ACP, 6 rounds in magazine', 'In Storage', 'ARMORY A-1-B');

-- Case 2025-004001 (Currency + Electronics + Jewelry) — mixed
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-CURR-0004', @c10, @officer2, '2025-04-02', 'Currency', 'Cash - $5,100.00, bundled with rubber bands', 'Deposited', NULL),
    ('2025-ELEC-0005', @c10, @officer2, '2025-04-02', 'Electronics', 'iPad Air 5th gen, gray, cracked, no passcode', 'In Storage', 'CPD B-3-A'),
    ('2025-JEWL-0003', @c10, @officer2, '2025-04-02', 'Jewelry', 'Rolex watch (replica), silver/black face', 'In Storage', 'CPD B-3-B');

-- Case 2025-004002 (Narcotics + Firearm) — firearm in armory, narcotics in storage
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-NARC-0008', @c11, @officer2, '2025-04-11', 'Narcotics', 'Suspected heroin, brown powder, approx 4g', 'In Storage', 'CPD C-4-A'),
    ('2025-FIRE-0004', @c11, @officer2, '2025-04-11', 'Firearm', 'Taurus G3, 9mm, S/N: TAU9G2025D, no magazine', 'In Storage', 'ARMORY B-2-A'),
    ('2025-NARC-0009', @c11, @officer2, '2025-04-11', 'Narcotic Equipment', 'Tin foil strips with residue, 12 pieces', 'In Storage', 'CPD C-4-B');

-- Case 2025-004003 (Weapon + Biological) — new intake, still in dropbox
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-WEAP-0003', @c12, @officer1, '2025-04-19', 'Weapon', 'Machete, 18-inch blade, dried blood', 'In Dropbox', 'Evidence Drop Box'),
    ('2025-BIO-0004',  @c12, @officer1, '2025-04-19', 'Biological / DNA', 'Blood swab from machete handle, Item A', 'In Dropbox', 'Evidence Drop Box'),
    ('2025-BIO-0005',  @c12, @officer1, '2025-04-19', 'Biological / DNA', 'Hair sample collected from scene', 'In Dropbox', 'Evidence Drop Box');

-- Case 2025-005001 (Electronics + Currency + Narcotics) — closed out cases
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-ELEC-0006', @c13, @officer1, '2025-05-06', 'Electronics', 'Google Pixel 8, cracked screen, white', 'Returned to Owner', NULL),
    ('2025-CURR-0005', @c13, @officer1, '2025-05-06', 'Currency', 'Cash - $320.00 mixed denomination', 'Deposited', NULL),
    ('2025-NARC-0010', @c13, @officer1, '2025-05-06', 'Narcotics', 'Suspected marijuana edibles, 6 gummy packs', 'Destroyed', NULL);

-- Case 2025-005002 (Firearm + Ammunition + Electronics) — firearms in armory, electronics in storage
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-FIRE-0005', @c14, @officer2, '2025-05-18', 'Firearm', 'Sig Sauer P320, 9mm, S/N: SIG9P2025E, loaded', 'In Storage', 'ARMORY C-1-A'),
    ('2025-AMMO-0004', @c14, @officer2, '2025-05-18', 'Ammunition', '9mm, 17 rounds in magazine', 'In Storage', 'ARMORY C-1-B'),
    ('2025-ELEC-0007', @c14, @officer2, '2025-05-18', 'Electronics', 'Apple Watch Series 9, black band, scratched face', 'In Storage', 'CPD D-5-A'),
    ('2025-ELEC-0008', @c14, @officer2, '2025-05-18', 'Electronics', 'Ring doorbell camera, removed from bracket', 'In Storage', 'CPD D-5-B');

-- Case 2025-005003 (Jewelry + Weapon + Biological) — weapons locker intake + some in storage
INSERT INTO evidence (barcode, case_id, collected_by_officer_id, collection_date, evidence_type, description, status, storage_location) VALUES
    ('2025-JEWL-0004', @c15, @officer1, '2025-05-29', 'Jewelry', 'Diamond stud earrings (pair), approx 0.5ct each', 'In Storage', 'CPD E-3-A'),
    ('2025-JEWL-0005', @c15, @officer1, '2025-05-29', 'Jewelry', 'Gold chain necklace, 24 inches, 14k', 'In Storage', 'CPD E-3-B'),
    ('2025-WEAP-0004', @c15, @officer1, '2025-05-29', 'Weapon', 'Taser cartridge (used), model 7 CQ', 'In Dropbox', 'Weapons Locker 3'),
    ('2025-BIO-0006',  @c15, @officer1, '2025-05-29', 'Biological / DNA', 'Fingernail scrapings, victim - sealed envelope', 'In Storage', 'CPD H-2-A');

-- -------------------------
-- Summary
-- -------------------------
SELECT 'Test data loaded successfully.' AS result;
SELECT COUNT(*) AS cases_added FROM cases WHERE case_number LIKE '2025-%';
SELECT COUNT(*) AS evidence_added FROM evidence WHERE barcode LIKE '2025-%';
SELECT status, COUNT(*) AS count FROM evidence WHERE barcode LIKE '2025-%' GROUP BY status ORDER BY status;
