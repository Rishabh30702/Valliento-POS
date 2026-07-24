package com.valliento.db;

import com.valliento.model.CartItem;
import com.valliento.model.SaleRecord;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    public static String determineShift() {
        int hour = LocalTime.now().getHour();
        return (hour >= 6 && hour < 18) ? "Day" : "Night";
    }

    public static String generateNextInvoiceNo() {
        String sql = "SELECT MAX(CAST(SUBSTR(invoice_no, 5) AS SIGNED)) AS max_no " +
                     "FROM sales WHERE invoice_no LIKE 'INV-%'";
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
        return "INV-" + nextNumber;
    }

    public static void recordSale(String invoiceNo, double total, double tax, List<CartItem> items, String cashierName) {
        recordSale(invoiceNo, total, tax, items, cashierName, false, "Cash");
    }

    public static void recordSale(String invoiceNo, double total, double tax, List<CartItem> items,
                                   String cashierName, boolean isInterState) {
        recordSale(invoiceNo, total, tax, items, cashierName, isInterState, "Cash");
    }

    public static void recordSale(String invoiceNo, double total, double tax, List<CartItem> items,
                                   String cashierName, boolean isInterState, String paymentMethod) {
        double cgstAmount = isInterState ? 0.0 : tax / 2.0;
        double sgstAmount = isInterState ? 0.0 : tax / 2.0;
        double igstAmount = isInterState ? tax : 0.0;
        String saleType = isInterState ? "Inter-State" : "Intra-State";
        String payMethod = (paymentMethod == null || paymentMethod.isBlank()) ? "Cash" : paymentMethod;

        // Use the app's own clock instead of the DB server's NOW(), so "today"
        // always matches the machine running the POS regardless of which
        // timezone the MySQL host happens to be running in.
        String createdAt = DatabaseManager.nowLocal();

        String insertSale = "INSERT INTO sales " +
            "(invoice_no, total, tax, cashier_name, shift, sale_type, cgst_amount, sgst_amount, igst_amount, payment_method, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItem = "INSERT INTO sale_items (sale_id, product_name, qty, price, gst_rate, gst_amount) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            long saleId;
            try (PreparedStatement ps = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, invoiceNo);
                ps.setDouble(2, total);
                ps.setDouble(3, tax);
                ps.setString(4, cashierName == null || cashierName.isBlank() ? "Unknown" : cashierName.trim());
                ps.setString(5, determineShift());
                ps.setString(6, saleType);
                ps.setDouble(7, cgstAmount);
                ps.setDouble(8, sgstAmount);
                ps.setDouble(9, igstAmount);
                ps.setString(10, payMethod);
                ps.setString(11, createdAt);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    saleId = keys.getLong(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                for (CartItem item : items) {
                    ps.setLong(1, saleId);
                    ps.setString(2, item.getName());
                    ps.setInt(3, item.getQty());
                    ps.setDouble(4, item.getPrice());
                    ps.setDouble(5, item.getGstRate());
                    ps.setDouble(6, item.getGstAmount());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException autoCommitEx) {
                    autoCommitEx.printStackTrace();
                }
            }
        }
    }

    public static double getTodaysSalesTotal() {
        // Compare against the app's own "today" (LocalDate.now()) rather than
        // the DB server's CURDATE(), which may be on a different clock/timezone.
        String sql = "SELECT COALESCE(SUM(total), 0) FROM sales WHERE DATE(created_at) = ?";
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

    public static int getTodaysTransactionCount() {
        String sql = "SELECT COUNT(*) FROM sales WHERE DATE(created_at) = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static List<SaleRecord> getSalesReport(String fromDate, String toDate) {
        List<SaleRecord> records = new ArrayList<>();
        String sql = "SELECT invoice_no, cashier_name, shift, total, created_at " +
                     "FROM sales WHERE DATE(created_at) BETWEEN ? AND ? ORDER BY created_at DESC";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new SaleRecord(
                        rs.getString("invoice_no"),
                        rs.getString("cashier_name"),
                        rs.getString("shift"),
                        rs.getDouble("total"),
                        rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }
}