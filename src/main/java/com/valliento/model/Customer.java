package com.valliento.model;

public class Customer {
    private int id;
    private String name;
    private String phone;
    private String email;
    private double totalPurchase;
    private int points;
    private String status;

    public Customer(int id, String name, String phone, String email,
                     double totalPurchase, int points, String status) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.totalPurchase = totalPurchase;
        this.points = points;
        this.status = status;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public double getTotalPurchase() { return totalPurchase; }
    public int getPoints() { return points; }
    public String getStatus() { return status; }

    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setStatus(String status) { this.status = status; }
}