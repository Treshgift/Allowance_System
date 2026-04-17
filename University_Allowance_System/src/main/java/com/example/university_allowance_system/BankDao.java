package com.example.university_allowance_system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BankDao {

    public String getBankNameById(int bankId) {
        String sql = "SELECT name FROM banks WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bankId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }

        } catch (Exception e) {
            java.util.logging.Logger.getLogger(BankDao.class.getName()).log(java.util.logging.Level.WARNING, "BankDao.getBankNameById failed", e);
        }

        return null;
    }
}