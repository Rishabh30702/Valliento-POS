package com.valliento.db;

import com.valliento.model.KotItem;
import com.valliento.model.KotOrder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KotDAO {

    public static int createKot(Integer tableId, String tableNo, Map<String, Integer> items) {
        String kotNo = "KOT-" + (System.currentTimeMillis() % 100000);
        String insertKot = "INSERT INTO kot_orders (kot_no, table_id, table_no, status, created_at) VALUES (?, ?, ?, 'New', ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(insertKot, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, kotNo);
            if (tableId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, tableId);
            ps.setString(3, tableNo);
            ps.setString(4, DatabaseManager.nowLocal());
            ps.executeUpdate();

            int kotId;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    kotId = keys.getInt(1);
                } else {
                    return -1;
                }
            }

            String insertItem = "INSERT INTO kot_items (kot_id, product_name, qty) VALUES (?, ?, ?)";
            try (PreparedStatement itemPs = DatabaseManager.getConnection().prepareStatement(insertItem)) {
                for (Map.Entry<String, Integer> entry : items.entrySet()) {
                    itemPs.setInt(1, kotId);
                    itemPs.setString(2, entry.getKey());
                    itemPs.setInt(3, entry.getValue());
                    itemPs.executeUpdate();
                }
            }
            return kotId;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static KotOrder getActiveKotForTable(int tableId) {
        String sql = "SELECT id, kot_no, table_id, table_no, status, created_at FROM kot_orders " +
                     "WHERE table_id = ? AND status != 'Served' ORDER BY created_at DESC, id DESC LIMIT 1";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean replaceItemsForKot(int kotId, Map<String, Integer> items) {
        String deleteSql = "DELETE FROM kot_items WHERE kot_id = ?";
        String insertSql = "INSERT INTO kot_items (kot_id, product_name, qty) VALUES (?, ?, ?)";
        try {
            Connection conn = DatabaseManager.getConnection();
            try (PreparedStatement delPs = conn.prepareStatement(deleteSql)) {
                delPs.setInt(1, kotId);
                delPs.executeUpdate();
            }
            try (PreparedStatement insPs = conn.prepareStatement(insertSql)) {
                for (Map.Entry<String, Integer> entry : items.entrySet()) {
                    insPs.setInt(1, kotId);
                    insPs.setString(2, entry.getKey());
                    insPs.setInt(3, entry.getValue());
                    insPs.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<KotOrder> getAllKots() {
        List<KotOrder> orders = new ArrayList<>();
        String sql = "SELECT id, kot_no, table_id, table_no, status, created_at FROM kot_orders " +
                     "ORDER BY created_at DESC, id DESC";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                orders.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public static List<KotItem> getItemsForKot(int kotId) {
        List<KotItem> items = new ArrayList<>();
        String sql = "SELECT id, kot_id, product_name, qty FROM kot_items WHERE kot_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, kotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new KotItem(rs.getInt("id"), rs.getInt("kot_id"), rs.getString("product_name"), rs.getInt("qty")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static boolean updateKotStatus(int id, String status) {
        String sql = "UPDATE kot_orders SET status = ? WHERE id = ?";
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

    private static KotOrder mapRow(ResultSet rs) throws SQLException {
        int tableIdVal = rs.getInt("table_id");
        Integer tableId = rs.wasNull() ? null : tableIdVal;
        return new KotOrder(
            rs.getInt("id"),
            rs.getString("kot_no"),
            tableId,
            rs.getString("table_no"),
            rs.getString("status"),
            rs.getString("created_at")
        );
    }
}