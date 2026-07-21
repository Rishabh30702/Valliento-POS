package com.valliento.db;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:valliento.db";
    private static Connection connection;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static void initializeSchema() {
        String createProducts = """
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                price REAL NOT NULL,
                stock INTEGER DEFAULT 0
            )
        """;

        String createSales = """
            CREATE TABLE IF NOT EXISTS sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_no TEXT NOT NULL,
                total REAL NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createSaleItems = """
            CREATE TABLE IF NOT EXISTS sale_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                product_name TEXT NOT NULL,
                qty INTEGER NOT NULL,
                price REAL NOT NULL,
                FOREIGN KEY (sale_id) REFERENCES sales(id)
            )
        """;

        String createCustomers = """
            CREATE TABLE IF NOT EXISTS customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT,
                email TEXT,
                total_purchase REAL DEFAULT 0,
                points INTEGER DEFAULT 0,
                status TEXT DEFAULT 'Active'
            )
        """;

        String createSettings = """
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT
            )
        """;

        String createUsers = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                full_name TEXT NOT NULL,
                role TEXT DEFAULT 'Cashier'
            )
        """;

        String createSuppliers = """
            CREATE TABLE IF NOT EXISTS suppliers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT,
                email TEXT,
                total_purchase REAL DEFAULT 0,
                status TEXT DEFAULT 'Active'
            )
        """;

        String createExpenses = """
            CREATE TABLE IF NOT EXISTS expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                expense_type TEXT NOT NULL,
                amount REAL NOT NULL,
                note TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createEmployees = """
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                role TEXT NOT NULL,
                phone TEXT,
                status TEXT DEFAULT 'Active'
            )
        """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createProducts);
            stmt.execute(createSales);
            stmt.execute(createSaleItems);
            stmt.execute(createCustomers);
            stmt.execute(createSettings);

            String createPurchaseOrders = """
                CREATE TABLE IF NOT EXISTS purchase_orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    invoice_no TEXT NOT NULL,
                    supplier_id INTEGER NOT NULL,
                    supplier_name TEXT NOT NULL,
                    order_date TEXT NOT NULL,
                    amount REAL NOT NULL,
                    status TEXT DEFAULT 'Pending'
                )
            """;
            stmt.execute(createPurchaseOrders);

            String createRestaurantTables = """
                CREATE TABLE IF NOT EXISTS restaurant_tables (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    table_no TEXT NOT NULL UNIQUE,
                    status TEXT DEFAULT 'Available'
                )
            """;
            stmt.execute(createRestaurantTables);

            String createKotOrders = """
                CREATE TABLE IF NOT EXISTS kot_orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    kot_no TEXT NOT NULL,
                    table_id INTEGER,
                    table_no TEXT,
                    status TEXT DEFAULT 'New',
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP
                )
            """;
            stmt.execute(createKotOrders);

            String createKotItems = """
                CREATE TABLE IF NOT EXISTS kot_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    kot_id INTEGER NOT NULL,
                    product_name TEXT NOT NULL,
                    qty INTEGER NOT NULL,
                    FOREIGN KEY (kot_id) REFERENCES kot_orders(id)
                )
            """;
            stmt.execute(createKotItems);

            String createDailyClosings = """
                CREATE TABLE IF NOT EXISTS daily_closings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    closing_date TEXT NOT NULL UNIQUE,
                    total_sales REAL NOT NULL,
                    total_transactions INTEGER NOT NULL,
                    total_expenses REAL NOT NULL,
                    net_sales REAL NOT NULL,
                    closed_by TEXT,
                    closed_at TEXT DEFAULT CURRENT_TIMESTAMP
                )
            """;
            stmt.execute(createDailyClosings);
            seedTablesIfEmpty();
            stmt.execute(createUsers);
            stmt.execute(createSuppliers);
            stmt.execute(createExpenses);
            stmt.execute(createEmployees);
            seedDefaultUser();
            seedRoleDemoAccounts();

            seedProductsIfEmpty();
            migrateSalesTable();
            migrateProductsTable();
            migrateSaleItemsTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void seedTablesIfEmpty() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM restaurant_tables";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO restaurant_tables (table_no, status) VALUES (?, 'Available')";
                try (PreparedStatement ps = getConnection().prepareStatement(insert)) {
                    for (int i = 1; i <= 16; i++) {
                        String tableNo = String.format("T%02d", i);
                        ps.setString(1, tableNo);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    private static void seedProductsIfEmpty() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM products";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO products (name, category, price, stock) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = getConnection().prepareStatement(insert)) {
                    Object[][] seedData = {
                        {"Cappuccino", "Beverages", 120.00, 45},
                        {"Chocolate Pastry", "Pastries", 90.00, 28},
                        {"Veg Sandwich", "Snacks", 80.00, 32},
                        {"French Fries", "Snacks", 120.00, 25},
                        {"Cold Coffee", "Beverages", 110.00, 18},
                        {"Brownie", "Pastries", 70.00, 10},
                        {"Veg Burger", "Combo", 130.00, 15}
                    };
                    for (Object[] row : seedData) {
                        ps.setString(1, (String) row[0]);
                        ps.setString(2, (String) row[1]);
                        ps.setDouble(3, (Double) row[2]);
                        ps.setInt(4, (Integer) row[3]);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Ensures demo Manager / Cashier / Waiter accounts exist, even on databases
     * created before role-based login was added. Safe to run on every startup -
     * uses INSERT OR IGNORE so it never duplicates or overwrites existing users.
     */
    private static void seedRoleDemoAccounts() throws SQLException {
        String insert = "INSERT OR IGNORE INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(insert)) {
            ps.setString(1, "manager");
            ps.setString(2, "manager123");
            ps.setString(3, "Manager User");
            ps.setString(4, "Manager");
            ps.executeUpdate();

            ps.setString(1, "cashier");
            ps.setString(2, "cashier123");
            ps.setString(3, "Cashier User");
            ps.setString(4, "Cashier");
            ps.executeUpdate();

            ps.setString(1, "waiter");
            ps.setString(2, "waiter123");
            ps.setString(3, "Waiter User");
            ps.setString(4, "Waiter");
            ps.executeUpdate();
        }
    }

    private static void seedDefaultUser() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = getConnection().prepareStatement(insert)) {
                    // Administrator - full access to everything
                    ps.setString(1, "admin");
                    ps.setString(2, "admin123");
                    ps.setString(3, "Aman Gia");
                    ps.setString(4, "Administrator");
                    ps.executeUpdate();

                    // Manager - products, inventory, purchase, employees, suppliers, expenses, reports
                    ps.setString(1, "manager");
                    ps.setString(2, "manager123");
                    ps.setString(3, "Manager User");
                    ps.setString(4, "Manager");
                    ps.executeUpdate();

                    // Cashier - billing / invoice generation
                    ps.setString(1, "cashier");
                    ps.setString(2, "cashier123");
                    ps.setString(3, "Cashier User");
                    ps.setString(4, "Cashier");
                    ps.executeUpdate();

                    // Waiter - table management + KOT
                    ps.setString(1, "waiter");
                    ps.setString(2, "waiter123");
                    ps.setString(3, "Waiter User");
                    ps.setString(4, "Waiter");
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void migrateSalesTable() {
        // SQLite has no "ADD COLUMN IF NOT EXISTS" - try each, ignore error if column already exists.
        // Column names here must match exactly what SaleDAO.recordSale() inserts into ("tax"),
        // otherwise inserts fail at runtime with "no such column" even though this migration
        // itself runs without error (it would just have added a differently-named column).
        tryAlter("ALTER TABLE sales ADD COLUMN cashier_name TEXT DEFAULT 'Unknown'");
        tryAlter("ALTER TABLE sales ADD COLUMN shift TEXT DEFAULT 'Day'");
        tryAlter("ALTER TABLE sales ADD COLUMN tax REAL DEFAULT 0");
        // Inter-state (IGST) vs intra-state (CGST+SGST) support.
        tryAlter("ALTER TABLE sales ADD COLUMN sale_type TEXT DEFAULT 'Intra-State'");
        tryAlter("ALTER TABLE sales ADD COLUMN cgst_amount REAL DEFAULT 0");
        tryAlter("ALTER TABLE sales ADD COLUMN sgst_amount REAL DEFAULT 0");
        tryAlter("ALTER TABLE sales ADD COLUMN igst_amount REAL DEFAULT 0");
    }

    /**
     * Adds the GST rate column to products for databases created before GST
     * support existed. gst_rate stores the percentage (5, 12, 18, 20...).
     * A value of 0 means "GST Exempt" (no tax charged on that product).
     */
    private static void migrateProductsTable() {
        tryAlter("ALTER TABLE products ADD COLUMN gst_rate REAL DEFAULT 5");
    }

    /**
     * Records the GST rate/amount that applied to each sold line item, for GST reporting/audit.
     * Column names here must match exactly what SaleDAO.recordSale() inserts into
     * ("gst_rate", "gst_amount").
     */
    private static void migrateSaleItemsTable() {
        tryAlter("ALTER TABLE sale_items ADD COLUMN gst_rate REAL DEFAULT 0");
        tryAlter("ALTER TABLE sale_items ADD COLUMN gst_amount REAL DEFAULT 0");
    }

    private static void tryAlter(String sql) {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            // Column likely already exists - safe to ignore
        }
    }

    /**
     * Current local date/time as a string, for created_at-style columns.
     * SQLite's own CURRENT_TIMESTAMP default is UTC, which shows the wrong
     * day/time for local users - this gives the machine's local time instead.
     */
    public static String nowLocal() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}