package com.valliento.db;

import com.valliento.model.Expense;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {

    public static List<Expense> getAllExpenses() {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT id, expense_type, amount, note, created_at FROM expenses ORDER BY created_at DESC";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                expenses.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expenses;
    }

    public static boolean addExpense(String type, double amount, String note) {
        String sql = "INSERT INTO expenses (expense_type, amount, note, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setDouble(2, amount);
            ps.setString(3, note);
            ps.setString(4, DatabaseManager.nowLocal());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteExpense(int id) {
        String sql = "DELETE FROM expenses WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static double getTodaysExpenseTotal() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE date(created_at) = date('now', 'localtime')";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static Expense mapRow(ResultSet rs) throws SQLException {
        return new Expense(
            rs.getInt("id"),
            rs.getString("expense_type"),
            rs.getDouble("amount"),
            rs.getString("note"),
            rs.getString("created_at")
        );
    }
}