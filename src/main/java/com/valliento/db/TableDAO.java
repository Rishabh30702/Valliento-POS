package com.valliento.db;

import com.valliento.model.RestaurantTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TableDAO {

    public static List<RestaurantTable> getAllTables() {
        List<RestaurantTable> tables = new ArrayList<>();
        String sql = "SELECT id, table_no, status FROM restaurant_tables ORDER BY table_no ASC";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(new RestaurantTable(rs.getInt("id"), rs.getString("table_no"), rs.getString("status")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tables;
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