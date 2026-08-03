package com.valliento.db;

import com.valliento.model.RestaurantTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TableDAO {

    public static List<RestaurantTable> getAllTables(int locationId) {
        List<RestaurantTable> tables = new ArrayList<>();
        String sql = "SELECT id, table_no, status FROM restaurant_tables WHERE location_id = ? ORDER BY table_no ASC";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(new RestaurantTable(rs.getInt("id"), rs.getString("table_no"), rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tables;
    }

    /** Admin/Manager add a new table. */
    public static boolean addTable(String tableNo, int locationId) {
        String sql = "INSERT INTO restaurant_tables (table_no, status, location_id) VALUES (?, 'Available', ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, tableNo.trim());
            ps.setInt(2, locationId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Admin/Manager delete a table - only within their own location. */
    public static boolean deleteTable(int tableId, int locationId) {
        String sql = "DELETE FROM restaurant_tables WHERE id = ? AND location_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setInt(2, locationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateTableStatus(int id, String status) {
        String sql = "UPDATE restaurant_tables SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
} 