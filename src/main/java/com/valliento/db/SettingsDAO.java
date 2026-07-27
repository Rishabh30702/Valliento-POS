package com.valliento.db;

import java.sql.*;

public class SettingsDAO {

    public static String get(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE `key` = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString("value");
                    return (val == null || val.isBlank()) ? defaultValue : val;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public static String getSetting(String key, String defaultValue) {
        return get(key, defaultValue);
    }

    public static void set(String key, String value) {
        String sql = "INSERT INTO settings (`key`, value) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE value = VALUES(value)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveSetting(String key, String value) {
        set(key, value);
    }

    public static void seedDefaultsIfMissing() {
        seedIfMissing("upi_id", "valliento.demo@upi");
        seedIfMissing("merchant_name", "Valliento POS");
        seedIfMissing("merchant_phone", "9999999999");
    }

    private static void seedIfMissing(String key, String defaultValue) {
        String sql = "INSERT IGNORE INTO settings (`key`, value) VALUES (?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, defaultValue);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}