-- Schema for Evidence Management System

-- Cases table
CREATE TABLE IF NOT EXISTS Cases (
    caseNumber TEXT PRIMARY KEY,
    description TEXT,
    officer TEXT,
    date TEXT,  -- ISO format YYYY-MM-DD
    location TEXT,
    notes TEXT
);

-- Evidence table
CREATE TABLE IF NOT EXISTS Evidence (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    caseNumber TEXT NOT NULL,
    description TEXT,
    officer TEXT,
    date TEXT,  -- ISO format
    location TEXT,
    storageLocation TEXT,
    status TEXT DEFAULT 'In Dropbox',  -- 'In Dropbox', 'Stored', 'Checked Out', etc.
    notes TEXT,
    FOREIGN KEY (caseNumber) REFERENCES Cases(caseNumber)
);

-- Chain of Custody table
CREATE TABLE IF NOT EXISTS ChainOfCustody (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    evidenceId INTEGER NOT NULL,
    dateTime TEXT,  -- ISO format with time
    fromPerson TEXT,
    toPerson TEXT,
    action TEXT,  -- 'Transfer', 'Check In', 'Check Out', etc.
    notes TEXT,
    FOREIGN KEY (evidenceId) REFERENCES Evidence(id)
);