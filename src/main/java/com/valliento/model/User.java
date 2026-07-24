package com.valliento.model;

public class User {
    private final int id;
    private final String username;
    private final String fullName;
    private final String role;
    private final int locationId;

    /** Backward-compatible constructor - defaults locationId to the Main Location (1). */
    public User(int id, String username, String fullName, String role) {
        this(id, username, fullName, role, 1);
    }

    public User(int id, String username, String fullName, String role, int locationId) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.locationId = locationId;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public int getLocationId() { return locationId; }
}