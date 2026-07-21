package com.valliento.db;

import com.valliento.model.PurchaseOrder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDAO {

    /**
     * Generates the next sequential purchase invoice number (e.g. PUR-10001, PUR-10002...).
     * Looks at the highest existing numeric suffix so it keeps incrementing correctly
     * even if some orders were deleted.
     */
    public static String generateNextInvoiceNo() {
        String sql = "SELECT MAX(CAST(SUBSTR(invoice_no, 5) AS INTEGER)) AS max_no " +
                     "FROM purchase_orders WHERE invoice_no LIKE 'PUR-%'";
        int nextNumber = 10001;
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int maxNo = rs.getInt("max_no");
                if (!rs.wasNull()) {
                    nextNumber = maxNo + 1;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "PUR-" + nextNumber;
    }

    /** Looks up a single purchase order by its invoice number (used to load an order for updating). */
    public static PurchaseOrder findByInvoiceNo(String invoiceNo) {
        String sql = "SELECT id, invoice_no, supplier_id, supplier_name, order_date, amount, status " +
                     "FROM purchase_orders WHERE invoice_no = ? COLLATE NOCASE";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, invoiceNo.trim());
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

    public static List<PurchaseOrder> getAllPurchaseOrders() {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = "SELECT id, invoice_no, supplier_id, supplier_name, order_date, amount, status " +
                     "FROM purchase_orders ORDER BY order_date DESC, id DESC";
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

    public static boolean addPurchaseOrder(String invoiceNo, int supplierId, String supplierName,
                                            String orderDate, double amount, String status) {
        String sql = "INSERT INTO purchase_orders (invoice_no, supplier_id, supplier_name, order_date, amount, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, invoiceNo);
            ps.setInt(2, supplierId);
            ps.setString(3, supplierName);
            ps.setString(4, orderDate);
            ps.setDouble(5, amount);
            ps.setString(6, status);
            ps.executeUpdate();
            if ("Completed".equals(status)) {
                bumpSupplierTotal(supplierId, amount);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updatePurchaseOrder(int id, String invoiceNo, int supplierId, String supplierName,
                                               String orderDate, double amount, String status) {
        String selectOld = "SELECT status FROM purchase_orders WHERE id = ?";
        String oldStatus = null;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(selectOld)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) oldStatus = rs.getString("status");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sql = "UPDATE purchase_orders SET invoice_no = ?, supplier_id = ?, supplier_name = ?, " +
                     "order_date = ?, amount = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, invoiceNo);
            ps.setInt(2, supplierId);
            ps.setString(3, supplierName);
            ps.setString(4, orderDate);
            ps.setDouble(5, amount);
            ps.setString(6, status);
            ps.setInt(7, id);
            ps.executeUpdate();

            boolean wasCompleted = "Completed".equals(oldStatus);
            boolean nowCompleted = "Completed".equals(status);
            if (!wasCompleted && nowCompleted) {
                bumpSupplierTotal(supplierId, amount);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deletePurchaseOrder(int id) {
        String sql = "DELETE FROM purchase_orders WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void bumpSupplierTotal(int supplierId, double amount) {
        String sql = "UPDATE suppliers SET total_purchase = total_purchase + ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, supplierId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static PurchaseOrder mapRow(ResultSet rs) throws SQLException {
        return new PurchaseOrder(
            rs.getInt("id"),
            rs.getString("invoice_no"),
            rs.getInt("supplier_id"),
            rs.getString("supplier_name"),
            rs.getString("order_date"),
            rs.getDouble("amount"),
            rs.getString("status")
        );
    }
}