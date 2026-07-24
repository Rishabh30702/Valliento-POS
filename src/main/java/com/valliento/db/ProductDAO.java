package com.valliento.db;

import com.valliento.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public static List<Product> getAllProducts(int locationId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, name, category, price, stock, gst_rate FROM products WHERE location_id = ? ORDER BY name";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, locationId);
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

    public static int getTotalProductCount(int locationId) {
        String sql = "SELECT COUNT(*) FROM products WHERE location_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getLowStockCount(int threshold, int locationId) {
        String sql = "SELECT COUNT(*) FROM products WHERE stock < ? AND location_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, threshold);
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
     * Adds a new product with an explicit GST rate (percentage, e.g. 5.0, 12.0, 18.0, 20.0).
     * Use 0.0 for GST Exempt. Scoped to the given location_id.
     */
    public static boolean addProduct(String name, String category, double price, int stock, double gstRate, int locationId) {
        String sql = "INSERT INTO products (name, category, price, stock, gst_rate, location_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.setInt(4, stock);
            ps.setDouble(5, gstRate);
            ps.setInt(6, locationId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates a product including its GST rate. Scoped to location_id so a
     * product from one business can never be edited by another business's staff,
     * even if they somehow guessed its id.
     */
    public static boolean updateProduct(int id, String name, String category, double price, int stock, double gstRate, int locationId) {
        String sql = "UPDATE products SET name = ?, category = ?, price = ?, stock = ?, gst_rate = ? WHERE id = ? AND location_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.setInt(4, stock);
            ps.setDouble(5, gstRate);
            ps.setInt(6, id);
            ps.setInt(7, locationId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteProduct(int id, int locationId) {
        String sql = "DELETE FROM products WHERE id = ? AND location_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, locationId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean adjustStock(int id, int changeAmount, int locationId) {
        String sql = "UPDATE products SET stock = stock + ? WHERE id = ? AND location_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, changeAmount);
            ps.setInt(2, id);
            ps.setInt(3, locationId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Product> getLowStockProducts(int threshold, int locationId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, name, category, price, stock, gst_rate FROM products WHERE stock < ? AND location_id = ? ORDER BY stock ASC";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, threshold);
            ps.setInt(2, locationId);
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