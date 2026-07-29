package com.valliento.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CategoryDAO {

    /**
     * All categories explicitly created for this location (via the "+" control),
     * merged with any categories already in use by products - so nothing gets
     * lost even if a category was only ever typed on a product before this
     * table existed.
     */
    public static List<String> getAllCategoryNames(int locationId) {
        Set<String> names = new LinkedHashSet<>();

        String sql = "SELECT name FROM categories WHERE location_id = ? ORDER BY name";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        names.addAll(ProductDAO.getAllCategories(locationId));
        return new ArrayList<>(names);
    }

    /**
     * Adds a new category directly (independent of any product). Returns false
     * if it already exists for this location (duplicate) or on any DB error,
     * so the controller can tell the user why nothing changed.
     */
    public static boolean addCategory(String name, int locationId) {
        String sql = "INSERT IGNORE INTO categories (name, location_id) VALUES (?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, locationId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}