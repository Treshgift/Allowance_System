package com.example.university_allowance_system;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDao {

    public List<AdminStudentRow> getAllStudents(String keyword) {
        List<AdminStudentRow> students = new ArrayList<>();

        String sql = """
                SELECT s.id, s.student_code, s.full_name, b.name AS bank_name, s.status
                FROM students s
                JOIN banks b ON s.assigned_bank_id = b.id
                WHERE s.full_name LIKE ? OR s.student_code LIKE ? OR b.name LIKE ?
                ORDER BY s.full_name
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String search = "%" + keyword + "%";
            stmt.setString(1, search);
            stmt.setString(2, search);
            stmt.setString(3, search);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                students.add(new AdminStudentRow(
                        rs.getInt("id"),
                        rs.getString("student_code"),
                        rs.getString("full_name"),
                        rs.getString("bank_name"),
                        rs.getString("status")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return students;
    }

    public boolean addStudent(String studentCode, String fullName, String bankName, String status) {

        String insertStudent = """
        INSERT INTO students(student_code, full_name, assigned_bank_id, status)
        VALUES (?, ?, (SELECT id FROM banks WHERE name=?), ?)
    """;

        String createUser = """
        INSERT INTO users(username, password, role, student_id)
        VALUES (?, '1234', 'Student', ?)
    """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                int studentId;

                // ✅ Insert student and get ID directly
                try (PreparedStatement ps = conn.prepareStatement(insertStudent, PreparedStatement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, studentCode);
                    ps.setString(2, fullName);
                    ps.setString(3, bankName);
                    ps.setString(4, status);

                    ps.executeUpdate();

                    ResultSet rs = ps.getGeneratedKeys();

                    if (rs.next()) {
                        studentId = rs.getInt(1);
                    } else {
                        throw new Exception("Failed to get student ID");
                    }
                }

                // ✅ Create login account
                try (PreparedStatement ps = conn.prepareStatement(createUser)) {

                    ps.setString(1, studentCode); // username = student_code
                    ps.setInt(2, studentId);

                    ps.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (Exception e) {

                conn.rollback(); // 🔥 CRITICAL FIX

                System.out.println("Add student failed → rollback executed");
                e.printStackTrace();

                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void deleteStudent(String studentCode) {

        String getIdSql = "SELECT id FROM students WHERE student_code=?";
        String deleteUser = "DELETE FROM users WHERE student_id=?";
        String deleteStudent = "DELETE FROM students WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection()) {

            int studentId = 0;

            try (PreparedStatement ps = conn.prepareStatement(getIdSql)) {
                ps.setString(1, studentCode);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    studentId = rs.getInt("id");
                }
            }

            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(deleteUser)) {
                ps.setInt(1, studentId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(deleteStudent)) {
                ps.setInt(1, studentId);
                ps.executeUpdate();
            }

            conn.commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateStudentName(String studentCode, String newName) {

        String sql = "UPDATE students SET full_name=? WHERE student_code=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newName);
            ps.setString(2, studentCode);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean addBank(String bankName) {

        String sql = "INSERT INTO banks(name) VALUES (?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bankName);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public String generateNextStudentCode() {

        String sql = """
        SELECT MAX(CAST(SUBSTR(student_code, 5) AS INTEGER)) AS max_code
        FROM students
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {

                int max = rs.getInt("max_code");

                // If table is empty → max = 0
                return String.format("STD-%03d", max + 1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "STD-001";
    }
    // COUNT STUDENTS
    public int countStudents() {

        String sql = "SELECT COUNT(*) FROM students";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }


    // COUNT BANKS
    public int countBanks() {

        String sql = "SELECT COUNT(*) FROM banks";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }


    // COUNT TRANSACTIONS
    public int countTransactions() {

        String sql = """
        SELECT
        (SELECT COUNT(*) FROM loans) +
        (SELECT COUNT(*) FROM allowances)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }


    // COUNT DENIED ACCESS ATTEMPTS
    public int countDeniedAccessAttempts() {

        String sql = "SELECT COUNT(*) FROM access_log WHERE allowed = 0";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }


    // GET BANK NAMES
    public List<String> getBankNames() {

        List<String> banks = new ArrayList<>();

        String sql = "SELECT name FROM banks ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                banks.add(rs.getString("name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return banks;
    }
    public List<BankRow> getAllBanks(String keyword) {

        List<BankRow> banks = new ArrayList<>();

        // Count students assigned to each bank (shows number of student users per bank)
        String sql = """
        SELECT b.id, b.name,
               COUNT(s.id) AS user_count
        FROM banks b
        LEFT JOIN students s ON b.id = s.assigned_bank_id
        WHERE b.name LIKE ?
        GROUP BY b.id, b.name
        ORDER BY b.name
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + keyword + "%");

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                banks.add(new BankRow(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("user_count")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return banks;
    }

    // GET ALL TRANSACTIONS (Loans + Allowances)
    public List<TransactionRow> getAllTransactions(String keyword) {
        List<TransactionRow> transactions = new ArrayList<>();

        String sql = """
            SELECT * FROM (
                SELECT l.id, s.student_code, s.full_name, 'LOAN' as txn_type,
                       l.amount, l.status, l.created_at
                FROM loans l
                JOIN students s ON l.student_id = s.id
                WHERE s.student_code LIKE ? OR s.full_name LIKE ?
                UNION ALL
                SELECT a.id, s.student_code, s.full_name, 'ALLOWANCE' as txn_type,
                       a.amount, a.status, a.created_at
                FROM allowances a
                JOIN students s ON a.student_id = s.id
                WHERE s.student_code LIKE ? OR s.full_name LIKE ?
            )
            ORDER BY created_at DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String search = "%" + (keyword == null ? "" : keyword) + "%";
            stmt.setString(1, search);
            stmt.setString(2, search);
            stmt.setString(3, search);
            stmt.setString(4, search);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                transactions.add(new TransactionRow(
                        rs.getInt("id"),
                        rs.getString("student_code"),
                        rs.getString("full_name"),
                        rs.getString("txn_type"),
                        rs.getDouble("amount"),
                        rs.getString("status"),
                        rs.getString("created_at")
                ));
            }

        } catch (Exception e) {
            System.err.println("Error loading transactions: " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }

    // GET ALL ACCESS LOG ENTRIES
    public List<AccessLogRow> getAllAccessLogs(String keyword) {
        List<AccessLogRow> logs = new ArrayList<>();

        String sql = """
            SELECT al.id, u.username, u.role,
                   COALESCE(s.full_name, 'System') as student_name,
                   al.action, al.allowed, al.timestamp,
                   COALESCE(b.name, '-') AS bank_name
            FROM access_log al
            LEFT JOIN users u ON al.user_id = u.id
            LEFT JOIN banks b ON u.bank_id = b.id
            LEFT JOIN students s ON al.student_id = s.id
            WHERE u.username LIKE ? OR s.full_name LIKE ? OR al.action LIKE ?
            ORDER BY al.timestamp DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String search = "%" + (keyword == null ? "" : keyword) + "%";
            stmt.setString(1, search);
            stmt.setString(2, search);
            stmt.setString(3, search);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                logs.add(new AccessLogRow(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("student_name"),
                        rs.getString("action"),
                        rs.getInt("allowed") == 1,
                        rs.getString("timestamp"),
                        rs.getString("bank_name")
                ));
            }

        } catch (Exception e) {
            System.err.println("Error loading access logs: " + e.getMessage());
            e.printStackTrace();
        }

        return logs;
    }

    // RECORD ACCESS ATTEMPT
    public void recordAccess(Integer userId, Integer studentId, String action, boolean allowed) {
        String sql = "INSERT INTO access_log(user_id, student_id, action, allowed) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (userId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, userId);
            if (studentId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, studentId);
            ps.setString(3, action);
            ps.setInt(4, allowed ? 1 : 0);

            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("Failed to record access log: " + e.getMessage());
            e.printStackTrace();
        }
    }
}