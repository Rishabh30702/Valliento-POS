package com.valliento.db;

import com.valliento.model.Expense;

import java.sql.*;
import java.time.LocalDate;
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

    /**
     * Expenses between fromDate and toDate (inclusive), newest first.
     * Used by the date-range filter on the Expenses screen.
     */
    public static List<Expense> getExpensesByDateRange(String fromDate, String toDate) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT id, expense_type, amount, note, created_at FROM expenses " +
                     "WHERE DATE(created_at) BETWEEN ? AND ? ORDER BY created_at DESC";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapRow(rs));
                }
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

    /**
     * Updates an existing expense's type/amount/note. created_at is left as-is
     * (the original entry date shouldn't change just because it was edited).
     */
    public static boolean updateExpense(int id, String type, double amount, String note) {
        String sql = "UPDATE expenses SET expense_type = ?, amount = ?, note = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setDouble(2, amount);
            ps.setString(3, note);
            ps.setInt(4, id);
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

    /**
     * Fixed: previously relied on SQLite's own date('now','localtime'), which
     * doesn't match the app's own clock (same reasoning as SaleDAO.getTodaysSalesTotal).
     * Now parameterized against LocalDate.now(), same pattern used everywhere else.
     */
    public static double getTodaysExpenseTotal() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE DATE(created_at) = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
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