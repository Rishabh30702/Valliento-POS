package com.valliento.db;

import com.valliento.model.User;

import java.sql.*;

public class UserDAO {

    public static User authenticate(String username, String password) {
        String sql = "SELECT id, username, full_name, role FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}