package com.example.university_allowance_system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class StudentDao {

    public static List<StudentRow> getStudentsForBank(int bankId) {
        return getStudentsForBank(bankId, "");
    }

    public static List<StudentRow> getStudentsForBank(int bankId, String keyword) {

        List<StudentRow> students = new ArrayList<>();

        String sql = """
                SELECT
                    s.id,
                    COALESCE(s.balance, 0) AS balance,
                    s.student_code,
                    s.full_name,
                    s.assigned_bank_id,

                    COALESCE((
                        SELECT 'K ' || CAST(l.amount AS INT)
                        FROM loans l
                        WHERE l.student_id = s.id
                        ORDER BY l.id DESC
                        LIMIT 1
                    ), 'K 0') AS loan_amount,

                    COALESCE((
                        SELECT l.status
                        FROM loans l
                        WHERE l.student_id = s.id
                        ORDER BY l.id DESC
                        LIMIT 1
                    ), 'No Loan') AS loan_status,

                    COALESCE((
                        SELECT a.month
                        FROM allowances a
                        WHERE a.student_id = s.id
                        ORDER BY a.id DESC
                        LIMIT 1
                    ), '-') AS latest_month,

                    COALESCE((
                        SELECT a.status
                        FROM allowances a
                        WHERE a.student_id = s.id
                        ORDER BY a.id DESC
                        LIMIT 1
                    ), '-') AS latest_allowance_status

                FROM students s
                WHERE (s.full_name LIKE ? OR s.student_code LIKE ?)
                ORDER BY s.full_name
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchValue = "%" + keyword + "%";

            stmt.setString(1, searchValue);
            stmt.setString(2, searchValue);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                int studentBank = rs.getInt("assigned_bank_id");
                boolean allowed = (studentBank == bankId);

                // Format balance as K <int>
                String balanceStr = "-";
                if (allowed) {
                    double bal = rs.getDouble("balance");
                    if (rs.wasNull()) {
                        balanceStr = "K 0";
                    } else {
                        balanceStr = "K " + (int) bal;
                    }
                }

                students.add(new StudentRow(
                        rs.getInt("id"),

                        // HIDE ID if not allowed
                        allowed ? rs.getString("student_code") : "🔒 RESTRICTED",

                        // Name always visible
                        rs.getString("full_name"),

                        // balance (visible only if allowed)
                        balanceStr,
                        allowed ? rs.getString("latest_month") : "-",
                        allowed ? rs.getString("latest_allowance_status") : "-",
                        allowed ? rs.getString("loan_amount") : "-",
                        allowed ? rs.getString("loan_status") : "-",

                        studentBank
                ));
            }

        } catch (Exception e) {
            java.util.logging.Logger.getLogger(StudentDao.class.getName()).log(java.util.logging.Level.WARNING, "getStudentsForBank failed", e);
        }

        return students;
    }

    public static int countStudentsForBank(int bankId) {
        String sql = "SELECT COUNT(*) FROM students WHERE assigned_bank_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bankId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            java.util.logging.Logger.getLogger(StudentDao.class.getName()).log(java.util.logging.Level.WARNING, "disburseAllowancesForBank failed", e);
        }

        return 0;
    }

    public static String getTotalDisbursedForBank(int bankId) {
        String sql = """
                SELECT COALESCE(SUM(a.amount), 0)
                FROM allowances a
                JOIN students s ON a.student_id = s.id
                WHERE s.assigned_bank_id = ?
                  AND a.status = 'Paid'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bankId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return "K " + (int) rs.getDouble(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "K 0";
    }

    public static int getPendingCountForBank(int bankId) {
        String sql = """
                SELECT COUNT(*)
                FROM allowances a
                JOIN students s ON a.student_id = s.id
                WHERE s.assigned_bank_id = ?
                  AND a.status = 'Pending'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bankId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static int disburseAllowancesForBank(int bankId, double amount, String month) {

        String selectStudents = """
        SELECT id FROM students WHERE assigned_bank_id = ?
    """;

        String checkSql = """
        SELECT 1 FROM allowances 
        WHERE student_id = ? AND month = ?
    """;

        String insertSql = """
        INSERT INTO allowances(student_id, month, amount, status)
        VALUES (?, ?, ?, 'Paid')
    """;

        // 🔥 NEW: update balance
        String updateBalanceSql = """
        UPDATE students SET balance = COALESCE(balance, 0) + ?
        WHERE id = ?
    """;

        int count = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            PreparedStatement psStudents = conn.prepareStatement(selectStudents);
            psStudents.setInt(1, bankId);

            ResultSet rsStudents = psStudents.executeQuery();

            while (rsStudents.next()) {

                int studentId = rsStudents.getInt("id");

                //  prevent duplicate month
                PreparedStatement psCheck = conn.prepareStatement(checkSql);
                psCheck.setInt(1, studentId);
                psCheck.setString(2, month);

                ResultSet rsCheck = psCheck.executeQuery();

                if (rsCheck.next()) continue;

                //  insert allowance
                PreparedStatement psInsert = conn.prepareStatement(insertSql);
                psInsert.setInt(1, studentId);
                psInsert.setString(2, month);
                psInsert.setDouble(3, amount);
                psInsert.executeUpdate();

                // 💰 update balance (THIS WAS MISSING)
                PreparedStatement psUpdate = conn.prepareStatement(updateBalanceSql);
                psUpdate.setDouble(1, amount);
                psUpdate.setInt(2, studentId);
                psUpdate.executeUpdate();

                count++;
            }

            conn.commit();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }
}