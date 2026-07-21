package com.valliento.db;

import com.valliento.model.Employee;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    public static List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT id, name, role, phone, status FROM employees ORDER BY name ASC";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                employees.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employees;
    }

    public static boolean addEmployee(String name, String role, String phone) {
        String sql = "INSERT INTO employees (name, role, phone, status) VALUES (?, ?, ?, 'Active')";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, role);
            ps.setString(3, phone);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateEmployee(int id, String name, String role, String phone, String status) {
        String sql = "UPDATE employees SET name = ?, role = ?, phone = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, role);
            ps.setString(3, phone);
            ps.setString(4, status);
            ps.setInt(5, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteEmployee(int id) {
        String sql = "DELETE FROM employees WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Employee mapRow(ResultSet rs) throws SQLException {
        return new Employee(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("role"),
            rs.getString("phone"),
            rs.getString("status")
        );
    }
}