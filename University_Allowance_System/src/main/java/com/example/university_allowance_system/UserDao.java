package com.example.university_allowance_system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Simple DAO for user authentication. Logs successful logins to help debugging.
 */
 
public class UserDao {
    private static final Logger LOGGER = Logger.getLogger(UserDao.class.getName());


    /**
     * Attempts login using DB.
     * Returns true if login is valid, and stores session in UserSession.
     */
    public boolean login(String username, String password, String role) {

        // normalize inputs to avoid mismatches caused by extra spaces or case
        if (username != null) username = username.trim();
        if (role != null) role = role.trim();

        String sql = """
            SELECT id, username, role, bank_id, student_id
            FROM users
            WHERE username = ? AND password = ? AND role = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String uname = rs.getString("username");
                    String r = rs.getString("role");

                    // Safely read nullable integer columns using getInt + wasNull
                    int bankRaw = rs.getInt("bank_id");
                    Integer bankId = rs.wasNull() ? null : bankRaw;

                    int studentRaw = rs.getInt("student_id");
                    Integer studentId = rs.wasNull() ? null : studentRaw;

                    // If the user is a Student but users.student_id is not set, try to resolve it
                    if (studentId == null && "Student".equalsIgnoreCase(r)) {
                        try (PreparedStatement ps2 = conn.prepareStatement("SELECT id FROM students WHERE student_code = ? LIMIT 1")) {
                            ps2.setString(1, uname);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    studentId = rs2.getInt("id");
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.warning(() -> "Failed to resolve student_id for username=" + uname + " : " + ex.getMessage());
                        }
                    }

                    UserSession.set(userId, uname, r, bankId, studentId);
                    // make final copies so they can be safely referenced in the lambda
                    final int fid = userId;
                    final String funame = uname;
                    final String frole = r;
                    final Integer fbank = bankId;
                    final Integer fstudent = studentId;
                    LOGGER.info(() -> String.format("Login success: userId=%d username=%s role=%s bankId=%s studentId=%s", fid, funame, frole, fbank, fstudent));
                    return true;
                }
            }

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "UserDao.login failed", e);
        }

        return false;
    }
}