package com.valliento.db;

import com.valliento.model.Supplier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {

    public static List<Supplier> getAllSuppliers() {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT id, name, phone, email, total_purchase, status FROM suppliers ORDER BY name ASC";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                suppliers.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suppliers;
    }

    public static List<Supplier> searchSuppliers(String query) {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT id, name, phone, email, total_purchase, status FROM suppliers " +
                     "WHERE name LIKE ? OR phone LIKE ? ORDER BY name ASC";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            String like = "%" + query + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    suppliers.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suppliers;
    }

    public static boolean addSupplier(String name, String phone, String email) {
        String sql = "INSERT INTO suppliers (name, phone, email, total_purchase, status) VALUES (?, ?, ?, 0, 'Active')";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateSupplier(int id, String name, String phone, String email) {
        String sql = "UPDATE suppliers SET name = ?, phone = ?, email = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setInt(4, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteSupplier(int id) {
        String sql = "DELETE FROM suppliers WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Supplier mapRow(ResultSet rs) throws SQLException {
        return new Supplier(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getDouble("total_purchase"),
            rs.getString("status")
        );
    }
}