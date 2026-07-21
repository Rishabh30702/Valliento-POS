package com.valliento.db;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportDAO {

    public static double getTotalSales(String startDate, String endDate) {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM sales WHERE substr(created_at,1,10) BETWEEN ? AND ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static int getTransactionCount(String startDate, String endDate) {
        String sql = "SELECT COUNT(*) FROM sales WHERE substr(created_at,1,10) BETWEEN ? AND ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Map<String, Double> getDailySales(String startDate, String endDate) {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT substr(created_at,1,10) AS day, SUM(total) AS total FROM sales " +
                     "WHERE substr(created_at,1,10) BETWEEN ? AND ? GROUP BY day ORDER BY day ASC";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("day"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}