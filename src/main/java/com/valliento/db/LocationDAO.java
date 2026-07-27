package com.valliento.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocationDAO {

    public static class Location {
        public final int id;
        public final String name;
        public Location(int id, String name) {
            this.id = id;
            this.name = name;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    public static List<Location> getAllLocations() {
        List<Location> locations = new ArrayList<>();
        String sql = "SELECT id, name FROM locations ORDER BY name";
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                locations.add(new Location(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return locations;
    }

    public static int createLocationWithAdmin(String locationName, String adminUsername,
                                               String adminPassword, String adminFullName) {
        String insertLocation = "INSERT INTO locations (name, subscription_active, subscription_expiry_date) " +
                                 "VALUES (?, 1, DATE_ADD(CURDATE(), INTERVAL 6 MONTH))";
        String insertAdmin = "INSERT INTO users (username, password, full_name, role, location_id) VALUES (?, ?, ?, 'Administrator', ?)";

        try {
            Connection conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            int locationId;
            try (PreparedStatement ps = conn.prepareStatement(insertLocation, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, locationName);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    locationId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertAdmin)) {
                ps.setString(1, adminUsername);
                ps.setString(2, adminPassword);
                ps.setString(3, adminFullName);
                ps.setInt(4, locationId);
                ps.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);
            return locationId;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
}