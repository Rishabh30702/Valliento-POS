package com.valliento.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public static class UserRow {
        public final int id;
        public final String username;
        public final String fullName;
        public final String role;

        public UserRow(int id, String username, String fullName, String role) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
        }
    }

    public static com.valliento.model.User authenticate(String username, String password) {
        String sql = "SELECT id, username, full_name, role, location_id FROM users " +
                     "WHERE username = ? AND password = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new com.valliento.model.User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getInt("location_id")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean createStaffUser(String username, String password, String fullName,
                                           String role, int locationId) {
        if (!"Cashier".equals(role) && !"Manager".equals(role)) {
            System.err.println("createStaffUser rejected: role must be 'Cashier' or 'Manager', got: " + role);
            return false;
        }

        String sql = "INSERT INTO users (username, password, full_name, role, location_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setString(4, role);
            ps.setInt(5, locationId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<UserRow> getStaffForLocation(int locationId) {
        List<UserRow> users = new ArrayList<>();
        String sql = "SELECT id, username, full_name, role FROM users " +
                     "WHERE location_id = ? AND role IN ('Cashier', 'Manager') " +
                     "ORDER BY role, username";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new UserRow(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public static boolean deleteStaffUser(int userId, int locationId) {
        String sql = "DELETE FROM users WHERE id = ? AND location_id = ? AND role IN ('Cashier', 'Manager')";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, locationId);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}