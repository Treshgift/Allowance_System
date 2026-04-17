-- Minimal demo data for University Allowance System
PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

-- Banks
INSERT OR IGNORE INTO banks(id, name) VALUES (1, 'FNB');
INSERT OR IGNORE INTO banks(id, name) VALUES (2, 'ZANACO');
INSERT OR IGNORE INTO banks(id, name) VALUES (3, 'ABC Bank');

-- Students
INSERT OR IGNORE INTO students(id, student_code, full_name, assigned_bank_id, status, balance)
VALUES (1, 'STD-001', 'Real June', 1, 'Active', 2100);

INSERT OR IGNORE INTO students(id, student_code, full_name, assigned_bank_id, status, balance)
VALUES (2, 'STD-002', 'John Doe', 2, 'Active', 1200);

-- Users (student logins)
INSERT OR IGNORE INTO users(id, username, password, role, bank_id, student_id)
VALUES (1, 'STD-001', 'password', 'Student', NULL, 1);

INSERT OR IGNORE INTO users(id, username, password, role, bank_id, student_id)
VALUES (2, 'STD-002', 'password', 'Student', NULL, 2);

-- Admin user
INSERT OR IGNORE INTO users(id, username, password, role)
VALUES (10, 'admin', 'admin', 'Admin');

-- Allowances
INSERT OR IGNORE INTO allowances(id, student_id, month, amount, status)
VALUES (1, 1, 'SEPTEMBER 2026', 750, 'Paid');
INSERT OR IGNORE INTO allowances(id, student_id, month, amount, status)
VALUES (2, 1, 'AUGUST 2026', 600, 'Paid');
INSERT OR IGNORE INTO allowances(id, student_id, month, amount, status)
VALUES (3, 1, 'MARCH 2026', 750, 'Paid');

COMMIT;

