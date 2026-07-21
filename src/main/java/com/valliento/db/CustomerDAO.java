package com.valliento.db;

import com.valliento.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public static List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT id, name, phone, email, total_purchase, points, status FROM customers ORDER BY name ASC";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                customers.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    public static List<Customer> searchCustomers(String query) {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT id, name, phone, email, total_purchase, points, status FROM customers " +
                     "WHERE name LIKE ? OR phone LIKE ? ORDER BY name ASC";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            String like = "%" + query + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    public static boolean addCustomer(String name, String phone, String email) {
        String sql = "INSERT INTO customers (name, phone, email, total_purchase, points, status) VALUES (?, ?, ?, 0, 0, 'Active')";
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

    public static boolean updateCustomer(int id, String name, String phone, String email) {
        String sql = "UPDATE customers SET name = ?, phone = ?, email = ? WHERE id = ?";
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

    public static boolean deleteCustomer(int id) {
        String sql = "DELETE FROM customers WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getDouble("total_purchase"),
            rs.getInt("points"),
            rs.getString("status")
        );
    }
}