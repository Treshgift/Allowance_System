package com.example.university_allowance_system;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    // ==================== CONSTANTS ====================
    private static final String BANK_ZANACO = "Zanaco";
    private static final String BANK_FNB = "FNB";

    private static final String STUDENT_CODE_001 = "STD-001";
    private static final String STUDENT_NAME_001 = "Mary Banda";
    private static final String STUDENT_CODE_002 = "STD-002";
    private static final String STUDENT_NAME_002 = "Tresh Gift";

    private static final String USER_ADMIN = "admin";
    private static final String DEFAULT_PASSWORD = "1234";

    private static final double LOAN_AMOUNT_001 = 4500;
    private static final double LOAN_AMOUNT_002 = 3200;

    private static final String LOAN_STATUS_APPROVED = "Approved";
    private static final String LOAN_STATUS_PENDING = "Pending";

    // ==================== INITIALIZATION ====================

    public static void initializeDatabase() {

        LOGGER.info("Starting database initialization...");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");

            createBanksTable(stmt);
            createStudentsTable(stmt);
            // Ensure migrations (add new columns to existing DBs)
            ensureStudentsBalanceColumn(stmt);
            createUsersTable(stmt);
            createLoansTable(stmt);
            createAllowancesTable(stmt);
            // ensure users.student_id is populated for student accounts (data migration)
            ensureUsersStudentIdMapping(stmt);
            // Ensure uniqueness to prevent duplicate allowances per student/month
            try {
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_allowances_student_month ON allowances(student_id, month);");
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not create unique index on allowances", ex);
            }
            createAccessLogTable(stmt);

            //  IMPORTANT: Only seed once
            if (isDatabaseEmpty(conn)) {
                seedDemo(stmt);
                LOGGER.info("Demo data seeded.");
            }

           // LOGGER.info("Database initialized successfully.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    private static void ensureStudentsBalanceColumn(Statement stmt) {
        try {
            // Try to select the balance column; this will fail if it doesn't exist
            stmt.executeQuery("SELECT balance FROM students LIMIT 1");
        } catch (Exception e) {
            try {
                // Add the column with a sensible default
                stmt.execute("ALTER TABLE students ADD COLUMN balance REAL DEFAULT 0");
                LOGGER.info("Added 'balance' column to students table (migration)");
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to add balance column migration", ex);
            }
        }

        // Backfill balances from allowances (in case balance was not maintained previously)
        try {
            stmt.execute("UPDATE students SET balance = COALESCE((SELECT SUM(amount) FROM allowances WHERE allowances.student_id = students.id AND status='Paid'), 0);");
            LOGGER.info("Backfilled students.balance from allowances");
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to backfill students.balance", ex);
        }
    }

    // ==================== TABLE CREATION ====================

    private static void createBanksTable(Statement stmt) throws Exception {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS banks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );
        """);
    }

    private static void createStudentsTable(Statement stmt) throws Exception {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS students (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_code TEXT NOT NULL UNIQUE,
                full_name TEXT NOT NULL,
                assigned_bank_id INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'Active',
                balance REAL DEFAULT 0, -- 🔥 ADDED COLUMN
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (assigned_bank_id) REFERENCES banks(id)
            );
        """);
    }

    private static void createUsersTable(Statement stmt) throws Exception {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('Admin','Bank','Student')),
                bank_id INTEGER,
                student_id INTEGER,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (bank_id) REFERENCES banks(id),
                FOREIGN KEY (student_id) REFERENCES students(id)
            );
        """);
    }

    private static void createLoansTable(Statement stmt) throws Exception {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS loans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                amount REAL NOT NULL,
                status TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (student_id) REFERENCES students(id)
            );
        """);
    }

    private static void createAllowancesTable(Statement stmt) throws Exception
    {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS allowances (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                month TEXT NOT NULL,
                amount REAL NOT NULL,
                status TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (student_id) REFERENCES students(id)
            );
        """);
    }
    private static void createAccessLogTable(Statement stmt) throws Exception {
        stmt.execute("""
        CREATE TABLE IF NOT EXISTS access_log (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            student_id INTEGER,
            action TEXT,
            allowed INTEGER, -- 🔥 REQUIRED COLUMN
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id),
            FOREIGN KEY (student_id) REFERENCES students(id)
        );
    """);
    }

    // ==================== SEED CHECK ====================

    private static boolean isDatabaseEmpty(Connection conn) {

        try (Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM students");

            if (rs.next()) {
                return rs.getInt(1) == 0;
            }

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "isDatabaseEmpty check failed", e);
        }

        return true;
    }

    // ==================== SEED DATA ====================

    private static void seedDemo(Statement stmt) throws Exception {

        stmt.execute("INSERT OR IGNORE INTO banks(name) VALUES ('" + BANK_ZANACO + "')");
        stmt.execute("INSERT OR IGNORE INTO banks(name) VALUES ('" + BANK_FNB + "')");

        stmt.execute(String.format("""
            INSERT INTO students(student_code, full_name, assigned_bank_id, status, balance)
            VALUES ('%s','%s',(SELECT id FROM banks WHERE name='%s'),'Active',0)
        """, STUDENT_CODE_001, STUDENT_NAME_001, BANK_ZANACO));

        stmt.execute(String.format("""
            INSERT INTO students(student_code, full_name, assigned_bank_id, status, balance)
            VALUES ('%s','%s',(SELECT id FROM banks WHERE name='%s'),'Active',0)
        """, STUDENT_CODE_002, STUDENT_NAME_002, BANK_FNB));

        stmt.execute(String.format("""
            INSERT INTO users(username,password,role)
            VALUES ('%s','%s','Admin')
        """, USER_ADMIN, DEFAULT_PASSWORD));

        stmt.execute(String.format("""
            INSERT INTO users(username,password,role,bank_id)
            VALUES ('%s','%s','Bank',(SELECT id FROM banks WHERE name='%s'))
        """, BANK_ZANACO.toLowerCase(), DEFAULT_PASSWORD, BANK_ZANACO));

        stmt.execute(String.format("""
            INSERT INTO users(username,password,role,bank_id)
            VALUES ('%s','%s','Bank',(SELECT id FROM banks WHERE name='%s'))
        """, BANK_FNB.toLowerCase(), DEFAULT_PASSWORD, BANK_FNB));

        stmt.execute(String.format("""
            INSERT INTO loans(student_id, amount, status)
            VALUES ((SELECT id FROM students WHERE student_code='%s'), %f, '%s')
        """, STUDENT_CODE_001, LOAN_AMOUNT_001, LOAN_STATUS_APPROVED));

        stmt.execute(String.format("""
            INSERT INTO loans(student_id, amount, status)
            VALUES ((SELECT id FROM students WHERE student_code='%s'), %f, '%s')
        """, STUDENT_CODE_002, LOAN_AMOUNT_002, LOAN_STATUS_PENDING));
        // 🔥 CREATE STUDENT USER ACCOUNTS

        stmt.execute(String.format("""
    INSERT INTO users(username,password,role,student_id)
    VALUES ('%s','%s','Student',
    (SELECT id FROM students WHERE student_code='%s'))
""", STUDENT_CODE_001, DEFAULT_PASSWORD, STUDENT_CODE_001));

        stmt.execute(String.format("""
    INSERT INTO users(username,password,role,student_id)
    VALUES ('%s','%s','Student',
    (SELECT id FROM students WHERE student_code='%s'))
""", STUDENT_CODE_002, DEFAULT_PASSWORD, STUDENT_CODE_002));
    }

    private static void ensureUsersStudentIdMapping(Statement stmt) {
        try {
            stmt.execute("UPDATE users SET student_id = (SELECT id FROM students WHERE students.student_code = users.username) WHERE role = 'Student' AND (student_id IS NULL OR student_id = 0);");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to populate users.student_id from students table", e);
        }
    }
}