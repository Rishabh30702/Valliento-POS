package com.valliento.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {

    public static final String STATUS_READY = "Ready to Use";
    public static final String STATUS_BOOKED = "Booked";
    public static final String STATUS_CLEANING = "Cleaning";

    public static class Room {
        public final int id;
        public final String roomNo;
        public final String status;
        public final Double price;

        public Room(int id, String roomNo, String status, Double price) {
            this.id = id;
            this.roomNo = roomNo;
            this.status = status;
            this.price = price;
        }
    }

    public static List<Room> getAllRooms(int locationId) {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT id, room_no, status, price FROM rooms WHERE location_id = ? ORDER BY room_no";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double p = rs.getDouble("price");
                    Double price = rs.wasNull() ? null : p;
                    rooms.add(new Room(rs.getInt("id"), rs.getString("room_no"), rs.getString("status"), price));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    public static boolean addRoom(String roomNo, int locationId) {
        String sql = "INSERT INTO rooms (room_no, status, location_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomNo.trim());
            ps.setString(2, STATUS_READY);
            ps.setInt(3, locationId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteRoom(int roomId, int locationId) {
        String sql = "DELETE FROM rooms WHERE id = ? AND location_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, locationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean bookRoom(int roomId, double price) {
        if (price <= 0) {
            return false;
        }
        String sql = "UPDATE rooms SET status = ?, price = ? WHERE id = ? AND status != ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, STATUS_BOOKED);
            ps.setDouble(2, price);
            ps.setInt(3, roomId);
            ps.setString(4, STATUS_BOOKED);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateRoomStatus(int roomId, String status) {
        if (STATUS_BOOKED.equals(status)) {
            return false;
        }
        String sql = "UPDATE rooms SET status = ?, price = NULL WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, roomId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String nextStatus(String current) {
        switch (current) {
            case STATUS_READY: return STATUS_BOOKED;
            case STATUS_BOOKED: return STATUS_CLEANING;
            case STATUS_CLEANING: return STATUS_READY;
            default: return STATUS_READY;
        }
    }
}