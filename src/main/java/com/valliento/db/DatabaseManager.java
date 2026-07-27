package com.valliento.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class DatabaseManager {

    private static final String DB_HOST;
    private static final String DB_PORT;
    private static final String DB_NAME;
    private static final String DB_USER;
    private static final String DB_PASSWORD;
    private static final String DB_URL;

    static {
        Properties fileProps = loadConfigFile();

        DB_HOST = resolve("DB_HOST", fileProps);
        DB_PORT = resolve("DB_PORT", fileProps, "3306");
        DB_NAME = resolve("DB_NAME", fileProps);
        DB_USER = resolve("DB_USER", fileProps);
        DB_PASSWORD = resolve("DB_PASSWORD", fileProps);

        if (DB_HOST == null || DB_NAME == null || DB_USER == null || DB_PASSWORD == null) {
            throw new IllegalStateException(
                "Missing database configuration. Set DB_HOST, DB_NAME, DB_USER, and "
                + "DB_PASSWORD as environment variables, or provide a db.properties "
                + "file (see DatabaseManager.loadConfigFile() for the expected location)."
            );
        }

        DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private static String resolve(String key, Properties fileProps) {
        return resolve(key, fileProps, null);
    }

    private static String resolve(String key, Properties fileProps, String defaultValue) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String propValue = fileProps.getProperty(key);
        if (propValue != null && !propValue.isBlank()) {
            return propValue;
        }
        return defaultValue;
    }

    private static Properties loadConfigFile() {
        Properties props = new Properties();
        Path configPath = Path.of("db.properties");
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Warning: failed to read db.properties: " + e.getMessage());
            }
        }
        return props;
    }

    private static Connection connection;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return connection;
    }

    /** The location_id assigned to all pre-existing/demo data and demo accounts. */
    public static final int DEFAULT_LOCATION_ID = 1;

    public static void initializeSchema() {
        String createLocations = """
            CREATE TABLE IF NOT EXISTS locations (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                address VARCHAR(255),
                phone VARCHAR(50),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB
        """;

        String createProducts = """
            CREATE TABLE IF NOT EXISTS products (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                category VARCHAR(255) NOT NULL,
                price DOUBLE NOT NULL,
                stock INT DEFAULT 0
            ) ENGINE=InnoDB
        """;

        String createSales = """
            CREATE TABLE IF NOT EXISTS sales (
                id INT AUTO_INCREMENT PRIMARY KEY,
                invoice_no VARCHAR(100) NOT NULL,
                total DOUBLE NOT NULL,
               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB
        """;

        String createSaleItems = """
            CREATE TABLE IF NOT EXISTS sale_items (
                id INT AUTO_INCREMENT PRIMARY KEY,
                sale_id INT NOT NULL,
                product_name VARCHAR(255) NOT NULL,
                qty INT NOT NULL,
                price DOUBLE NOT NULL,
                FOREIGN KEY (sale_id) REFERENCES sales(id)
            ) ENGINE=InnoDB
        """;

        String createCustomers = """
            CREATE TABLE IF NOT EXISTS customers (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                phone VARCHAR(50),
                email VARCHAR(255),
                total_purchase DOUBLE DEFAULT 0,
                points INT DEFAULT 0,
                status VARCHAR(50) DEFAULT 'Active'
            ) ENGINE=InnoDB
        """;

        String createSettings = """
            CREATE TABLE IF NOT EXISTS settings (
                `key` VARCHAR(191) PRIMARY KEY,
                value TEXT
            ) ENGINE=InnoDB
        """;

        // NOTE: username uniqueness is now per-location (added via migration below),
        // not global - see migrateUsersTable(). This lets Location A and Location B
        // each have their own "cashier1" without colliding.
        String createUsers = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(191) NOT NULL,
                password VARCHAR(255) NOT NULL,
                full_name VARCHAR(255) NOT NULL,
                role VARCHAR(50) DEFAULT 'Cashier'
            ) ENGINE=InnoDB
        """;

        String createSuppliers = """
            CREATE TABLE IF NOT EXISTS suppliers (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                phone VARCHAR(50),
                email VARCHAR(255),
                total_purchase DOUBLE DEFAULT 0,
                status VARCHAR(50) DEFAULT 'Active'
            ) ENGINE=InnoDB
        """;

        String createExpenses = """
            CREATE TABLE IF NOT EXISTS expenses (
                id INT AUTO_INCREMENT PRIMARY KEY,
                expense_type VARCHAR(255) NOT NULL,
                amount DOUBLE NOT NULL,
                note TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB
        """;

        String createEmployees = """
            CREATE TABLE IF NOT EXISTS employees (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                role VARCHAR(100) NOT NULL,
                phone VARCHAR(50),
                status VARCHAR(50) DEFAULT 'Active'
            ) ENGINE=InnoDB
        """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createLocations);
            stmt.execute(createProducts);
            stmt.execute(createSales);
            stmt.execute(createSaleItems);
            stmt.execute(createCustomers);
            stmt.execute(createSettings);

            String createPurchaseOrders = """
                CREATE TABLE IF NOT EXISTS purchase_orders (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    invoice_no VARCHAR(100) NOT NULL,
                    supplier_id INT NOT NULL,
                    supplier_name VARCHAR(255) NOT NULL,
                    order_date VARCHAR(50) NOT NULL,
                    amount DOUBLE NOT NULL,
                    status VARCHAR(50) DEFAULT 'Pending'
                ) ENGINE=InnoDB
            """;
            stmt.execute(createPurchaseOrders);

            String createRestaurantTables = """
                CREATE TABLE IF NOT EXISTS restaurant_tables (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    table_no VARCHAR(50) NOT NULL,
                    status VARCHAR(50) DEFAULT 'Available'
                ) ENGINE=InnoDB
            """;
            stmt.execute(createRestaurantTables);

            String createKotOrders = """
                CREATE TABLE IF NOT EXISTS kot_orders (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    kot_no VARCHAR(100) NOT NULL,
                    table_id INT,
                    table_no VARCHAR(50),
                    status VARCHAR(50) DEFAULT 'New',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """;
            stmt.execute(createKotOrders);

            String createKotItems = """
                CREATE TABLE IF NOT EXISTS kot_items (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    kot_id INT NOT NULL,
                    product_name VARCHAR(255) NOT NULL,
                    qty INT NOT NULL,
                    FOREIGN KEY (kot_id) REFERENCES kot_orders(id)
                ) ENGINE=InnoDB
            """;
            stmt.execute(createKotItems);

            String createDailyClosings = """
                CREATE TABLE IF NOT EXISTS daily_closings (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    closing_date VARCHAR(20) NOT NULL,
                    total_sales DOUBLE NOT NULL,
                    total_transactions INT NOT NULL,
                    total_expenses DOUBLE NOT NULL,
                    net_sales DOUBLE NOT NULL,
                    closed_by VARCHAR(255),
                    closed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """;
            stmt.execute(createDailyClosings);

            stmt.execute(createUsers);
            stmt.execute(createSuppliers);
            stmt.execute(createExpenses);
            stmt.execute(createEmployees);

            seedDefaultLocation();
            seedTablesIfEmpty();
            seedDefaultUser();
            seedRoleDemoAccounts();

            migrateSalesTable();
            migrateProductsTable();
            migrateSaleItemsTable();
            migrateLocationColumns();
            migrateUsersTable();
            migrateSubscriptionColumns();
            SettingsDAO.seedDefaultsIfMissing();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ensures a "Main Location" exists with id = DEFAULT_LOCATION_ID (1), so all
     * existing/demo data has somewhere to belong. New locations (e.g. "Agra
     * Location 2") get created later via LocationDAO.createLocation(...).
     */
    private static void seedDefaultLocation() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM locations";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO locations (id, name) VALUES (?, ?)";
                try (PreparedStatement ps = getConnection().prepareStatement(insert)) {
                    ps.setInt(1, DEFAULT_LOCATION_ID);
                    ps.setString(2, "Main Location");
                    ps.executeUpdate();
                }
            }
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

    /**
     * Ensures demo Manager / Cashier / Waiter accounts exist for the Main Location,
     * even on databases created before role-based login was added. Safe to run on
     * every startup - uses INSERT IGNORE, keyed off the composite
     * (username, location_id) uniqueness added in migrateUsersTable(), so it
     * never duplicates or overwrites existing users.
     */
    private static void seedRoleDemoAccounts() throws SQLException {
        String insert = "INSERT IGNORE INTO users (username, password, full_name, role, location_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(insert)) {
            ps.setString(1, "manager");
            ps.setString(2, "manager123");
            ps.setString(3, "Manager User");
            ps.setString(4, "Manager");
            ps.setInt(5, DEFAULT_LOCATION_ID);
            ps.executeUpdate();

            ps.setString(1, "cashier");
            ps.setString(2, "cashier123");
            ps.setString(3, "Cashier User");
            ps.setString(4, "Cashier");
            ps.setInt(5, DEFAULT_LOCATION_ID);
            ps.executeUpdate();

            ps.setString(1, "waiter");
            ps.setString(2, "waiter123");
            ps.setString(3, "Waiter User");
            ps.setString(4, "Waiter");
            ps.setInt(5, DEFAULT_LOCATION_ID);
            ps.executeUpdate();

            ps.setString(1, "cashier2");
            ps.setString(2, "cashier1234");
            ps.setString(3, "Cashier User 2");
            ps.setString(4, "Cashier");
            ps.setInt(5, DEFAULT_LOCATION_ID);
            ps.executeUpdate();
        } catch (SQLException e) {
            // If this runs before migrateUsersTable() has added location_id on an
            // upgrade path, location_id won't exist yet - safe to skip this pass;
            // it'll succeed on the next app startup after migration has run once.
        }
    }

    private static void seedDefaultUser() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = getConnection().prepareStatement(insert)) {
                    ps.setString(1, "admin");
                    ps.setString(2, "admin123");
                    ps.setString(3, "Aman Gia");
                    ps.setString(4, "Administrator");
                    ps.executeUpdate();

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
        }
    }

    private static void migrateSalesTable() {
        tryAlter("ALTER TABLE sales ADD COLUMN cashier_name VARCHAR(255) DEFAULT 'Unknown'");
        tryAlter("ALTER TABLE sales ADD COLUMN shift VARCHAR(50) DEFAULT 'Day'");
        tryAlter("ALTER TABLE sales ADD COLUMN tax DOUBLE DEFAULT 0");
        tryAlter("ALTER TABLE sales ADD COLUMN sale_type VARCHAR(50) DEFAULT 'Intra-State'");
        tryAlter("ALTER TABLE sales ADD COLUMN cgst_amount DOUBLE DEFAULT 0");
        tryAlter("ALTER TABLE sales ADD COLUMN sgst_amount DOUBLE DEFAULT 0");
        tryAlter("ALTER TABLE sales ADD COLUMN igst_amount DOUBLE DEFAULT 0");
        tryAlter("ALTER TABLE sales ADD COLUMN payment_method VARCHAR(50) DEFAULT 'Cash'");
    }

    private static void migrateProductsTable() {
        tryAlter("ALTER TABLE products ADD COLUMN gst_rate DOUBLE DEFAULT 5");
    }

    private static void migrateSaleItemsTable() {
        tryAlter("ALTER TABLE sale_items ADD COLUMN gst_rate DOUBLE DEFAULT 0");
        tryAlter("ALTER TABLE sale_items ADD COLUMN gst_amount DOUBLE DEFAULT 0");
    }

    /**
     * Adds location_id to every top-level business-data table, defaulting all
     * existing rows to DEFAULT_LOCATION_ID (Main Location) so nothing already
     * in the database becomes orphaned. sale_items and kot_items don't need
     * their own location_id - they're scoped through their parent (sales /
     * kot_orders) via sale_id / kot_id.
     */
    private static void migrateLocationColumns() {
        tryAlter("ALTER TABLE products ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
        tryAlter("ALTER TABLE sales ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
        tryAlter("ALTER TABLE customers ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
        tryAlter("ALTER TABLE suppliers ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
        tryAlter("ALTER TABLE expenses ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
        tryAlter("ALTER TABLE employees ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
        tryAlter("ALTER TABLE purchase_orders ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
        tryAlter("ALTER TABLE restaurant_tables ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
        tryAlter("ALTER TABLE kot_orders ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
        tryAlter("ALTER TABLE daily_closings ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
    }

    /**
     * Adds location_id to users and switches username uniqueness from GLOBAL
     * to PER-LOCATION. This is the critical fix: previously "cashier1" could
     * only ever exist once across your entire business. Now Agra Location 1
     * and Agra Location 2 can each independently have their own "cashier1"
     * without colliding, because the real uniqueness key is
     * (username, location_id) together, not username alone.
     */
    private static void migrateUsersTable() {
        tryAlter("ALTER TABLE users ADD COLUMN location_id INT DEFAULT " + DEFAULT_LOCATION_ID);
        tryAlter("ALTER TABLE users DROP INDEX username");
        tryAlter("ALTER TABLE users ADD UNIQUE KEY uniq_username_per_location (username, location_id)");
    }

    /**
     * Adds subscription tracking to locations: subscription_active (boolean)
     * and subscription_expiry_date. Every 6 months from signup/renewal, a
     * location's access expires - SubscriptionDAO.isSubscriptionValid()
     * checks this on every login attempt and auto-flips subscription_active
     * to false once the expiry date passes, blocking further logins until
     * SubscriptionDAO.renewSubscription(locationId) is called.
     *
     * Existing locations (created before this feature existed) are
     * "grandfathered" with a fresh 6-month window starting today, so nobody
     * already using the system gets locked out unexpectedly on this update.
     */
    private static void migrateSubscriptionColumns() {
        tryAlter("ALTER TABLE locations ADD COLUMN subscription_active TINYINT(1) DEFAULT 1");
        tryAlter("ALTER TABLE locations ADD COLUMN subscription_expiry_date DATE DEFAULT NULL");

        String backfillSql = "UPDATE locations SET subscription_expiry_date = DATE_ADD(CURDATE(), INTERVAL 6 MONTH) " +
                              "WHERE subscription_expiry_date IS NULL";
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(backfillSql);
        } catch (SQLException e) {
            // Column may not exist yet on a very first run before the ALTERs above
            // took effect in some edge case - safe to ignore, next startup catches it.
        }
    }

    private static void tryAlter(String sql) {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            // Column/index likely already exists, or doesn't exist to drop - safe to ignore
        }
    }

    public static String nowLocal() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}