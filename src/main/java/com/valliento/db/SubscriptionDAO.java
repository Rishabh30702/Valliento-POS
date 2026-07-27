package com.valliento.db;

import java.sql.*;
import java.time.LocalDate;

public class SubscriptionDAO {

    public static final int DEFAULT_SUBSCRIPTION_MONTHS = 6;

    public static boolean isSubscriptionValid(int locationId) {
        String sql = "SELECT subscription_active, subscription_expiry_date FROM locations WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                boolean active = rs.getBoolean("subscription_active");
                Date expiryDate = rs.getDate("subscription_expiry_date");

                if (expiryDate == null) {
                    return active;
                }

                LocalDate expiry = expiryDate.toLocalDate();
                boolean expired = expiry.isBefore(LocalDate.now());

                if (expired && active) {
                    deactivateSubscription(locationId);
                    return false;
                }

                return active && !expired;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static LocalDate getExpiryDate(int locationId) {
        String sql = "SELECT subscription_expiry_date FROM locations WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date d = rs.getDate("subscription_expiry_date");
                    return d != null ? d.toLocalDate() : null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void deactivateSubscription(int locationId) {
        String sql = "UPDATE locations SET subscription_active = 0 WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, locationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean renewSubscription(int locationId, int months) {
        String sql = "UPDATE locations SET subscription_active = 1, " +
                     "subscription_expiry_date = DATE_ADD(CURDATE(), INTERVAL ? MONTH) WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, months);
            ps.setInt(2, locationId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean renewSubscription(int locationId) {
        return renewSubscription(locationId, DEFAULT_SUBSCRIPTION_MONTHS);
    }
}