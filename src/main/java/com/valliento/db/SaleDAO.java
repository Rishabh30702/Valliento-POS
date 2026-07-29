package com.valliento.db;

import com.valliento.model.CartItem;
import com.valliento.model.SaleRecord;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {
    /**
     * Returns total sales grouped by month for the last N months (oldest first),
     * for charting on the Dashboard. Months with no sales still appear with 0.
     * Scoped to a single location_id.
     */
    public static java.util.LinkedHashMap<String, Double> getMonthlySalesTotals(int monthsBack, int locationId) {
        java.util.LinkedHashMap<String, Double> result = new java.util.LinkedHashMap<>();

        java.time.YearMonth current = java.time.YearMonth.now();
        java.util.List<java.time.YearMonth> months = new java.util.ArrayList<>();
        for (int i = monthsBack - 1; i >= 0; i--) {
            months.add(current.minusMonths(i));
        }
        for (java.time.YearMonth ym : months) {
            result.put(ym.format(java.time.format.DateTimeFormatter.ofPattern("MMM yy")), 0.0);
        }

        String sql = "SELECT strftime('%Y-%m', created_at) AS month, SUM(total) AS total " +
                     "FROM sales WHERE location_id = ? GROUP BY month ORDER BY month";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String monthKey = rs.getString("month");
                    double total = rs.getDouble("total");
                    try {
                        java.time.YearMonth ym = java.time.YearMonth.parse(monthKey);
                        String label = ym.format(java.time.format.DateTimeFormatter.ofPattern("MMM yy"));
                        if (result.containsKey(label)) {
                            result.put(label, total);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }


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

    public static void recordSale(String invoiceNo, double total, double tax, List<CartItem> items,
                                   String cashierName, int locationId) {
        recordSale(invoiceNo, total, tax, items, cashierName, false, "Cash", locationId);
    }

    public static void recordSale(String invoiceNo, double total, double tax, List<CartItem> items,
                                   String cashierName, boolean isInterState, int locationId) {
        recordSale(invoiceNo, total, tax, items, cashierName, isInterState, "Cash", locationId);
    }

    public static void recordSale(String invoiceNo, double total, double tax, List<CartItem> items,
                                   String cashierName, boolean isInterState, String paymentMethod, int locationId) {
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
            "(invoice_no, total, tax, cashier_name, shift, sale_type, cgst_amount, sgst_amount, igst_amount, payment_method, location_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                ps.setInt(11, locationId);
                ps.setString(12, createdAt);
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

    public static double getTodaysSalesTotal(int locationId) {
        // Compare against the app's own "today" (LocalDate.now()) rather than
        // the DB server's CURDATE(), which may be on a different clock/timezone.
        String sql = "SELECT COALESCE(SUM(total), 0) FROM sales WHERE DATE(created_at) = ? AND location_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getTodaysTransactionCount(int locationId) {
        String sql = "SELECT COUNT(*) FROM sales WHERE DATE(created_at) = ? AND location_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Item-wise sales report: one row per product sold, with CGST/SGST/IGST
     * derived from that sale's sale_type (Inter-State -> IGST, else CGST+SGST split).
     * Scoped to a single location_id.
     */
    public static List<com.valliento.model.SaleItemRecord> getItemWiseSalesReport(String fromDate, String toDate, int locationId) {
        List<com.valliento.model.SaleItemRecord> records = new ArrayList<>();
        String sql = "SELECT s.invoice_no, s.cashier_name, s.sale_type, s.created_at, " +
                     "si.product_name, si.qty, si.price, si.gst_rate, si.gst_amount " +
                     "FROM sale_items si JOIN sales s ON si.sale_id = s.id " +
                     "WHERE DATE(s.created_at) BETWEEN ? AND ? AND s.location_id = ? " +
                     "ORDER BY s.created_at DESC, si.id ASC";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            ps.setInt(3, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean interState = "Inter-State".equals(rs.getString("sale_type"));
                    double gstAmount = rs.getDouble("gst_amount");
                    double cgst = interState ? 0.0 : gstAmount / 2.0;
                    double sgst = interState ? 0.0 : gstAmount / 2.0;
                    double igst = interState ? gstAmount : 0.0;

                    records.add(new com.valliento.model.SaleItemRecord(
                        rs.getString("invoice_no"),
                        rs.getString("product_name"),
                        rs.getInt("qty"),
                        rs.getDouble("price"),
                        rs.getDouble("gst_rate"),
                        cgst, sgst, igst,
                        rs.getString("cashier_name"),
                        rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public static List<SaleRecord> getSalesReport(String fromDate, String toDate, int locationId) {
        List<SaleRecord> records = new ArrayList<>();
        String sql = "SELECT invoice_no, cashier_name, shift, total, created_at " +
                     "FROM sales WHERE DATE(created_at) BETWEEN ? AND ? AND location_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            ps.setInt(3, locationId);
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