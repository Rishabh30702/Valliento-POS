package com.valliento.db;

import com.valliento.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, name, category, price, stock, gst_rate FROM products ORDER BY name";
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(new Product(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("stock"),
                    rs.getDouble("gst_rate")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public static int getTotalProductCount() {
        String sql = "SELECT COUNT(*) FROM products";
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getLowStockCount(int threshold) {
        String sql = "SELECT COUNT(*) FROM products WHERE stock < ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Adds a new product with an explicit GST rate (percentage, e.g. 5.0, 12.0, 18.0, 20.0).
     * Use 0.0 for GST Exempt.
     */
    public static boolean addProduct(String name, String category, double price, int stock, double gstRate) {
        String sql = "INSERT INTO products (name, category, price, stock, gst_rate) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.setInt(4, stock);
            ps.setDouble(5, gstRate);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Backward-compatible overload for any existing callers that don't pass GST yet.
    // Defaults to 0.0 (GST Exempt).
    public static boolean addProduct(String name, String category, double price, int stock) {
        return addProduct(name, category, price, stock, 0.0);
    }

    /**
     * Updates a product including its GST rate (percentage, e.g. 5.0, 12.0, 18.0, 20.0).
     * Use 0.0 for GST Exempt.
     */
    public static boolean updateProduct(int id, String name, String category, double price, int stock, double gstRate) {
        String sql = "UPDATE products SET name = ?, category = ?, price = ?, stock = ?, gst_rate = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.setInt(4, stock);
            ps.setDouble(5, gstRate);
            ps.setInt(6, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Backward-compatible overload for any existing callers that don't pass GST yet.
    // Leaves the existing gst_rate untouched instead of overwriting it with 0.
    public static boolean updateProduct(int id, String name, String category, double price, int stock) {
        String sql = "UPDATE products SET name = ?, category = ?, price = ?, stock = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.setInt(4, stock);
            ps.setInt(5, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteProduct(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean adjustStock(int id, int changeAmount) {
        String sql = "UPDATE products SET stock = stock + ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, changeAmount);
            ps.setInt(2, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Product> getLowStockProducts(int threshold) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, name, category, price, stock, gst_rate FROM products WHERE stock < ? ORDER BY stock ASC";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("stock"),
                        rs.getDouble("gst_rate")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
}