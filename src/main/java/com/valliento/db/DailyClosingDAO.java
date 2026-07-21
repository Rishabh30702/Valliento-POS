package com.valliento.db;

import com.valliento.model.DailyClosing;

import java.sql.*;

public class DailyClosingDAO {

    public static double getSalesTotalForDate(String date) {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM sales WHERE date(created_at) = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static int getTransactionCountForDate(String date) {
        String sql = "SELECT COUNT(*) FROM sales WHERE date(created_at) = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static double getExpensesTotalForDate(String date) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE date(created_at) = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static DailyClosing getClosingForDate(String date) {
        String sql = "SELECT id, closing_date, total_sales, total_transactions, total_expenses, net_sales, closed_by, closed_at " +
                     "FROM daily_closings WHERE closing_date = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DailyClosing(
                        rs.getInt("id"),
                        rs.getString("closing_date"),
                        rs.getDouble("total_sales"),
                        rs.getInt("total_transactions"),
                        rs.getDouble("total_expenses"),
                        rs.getDouble("net_sales"),
                        rs.getString("closed_by"),
                        rs.getString("closed_at")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean closeDay(String date, double totalSales, int totalTransactions,
                                    double totalExpenses, double netSales, String closedBy) {
        String sql = "INSERT INTO daily_closings (closing_date, total_sales, total_transactions, total_expenses, net_sales, closed_by, closed_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setDouble(2, totalSales);
            ps.setInt(3, totalTransactions);
            ps.setDouble(4, totalExpenses);
            ps.setDouble(5, netSales);
            ps.setString(6, closedBy);
            ps.setString(7, DatabaseManager.nowLocal());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}