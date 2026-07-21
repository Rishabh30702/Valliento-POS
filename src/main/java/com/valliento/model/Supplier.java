package com.valliento.model;

public class Supplier {
    private int id;
    private String name;
    private String phone;
    private String email;
    private double totalPurchase;
    private String status;

    public Supplier(int id, String name, String phone, String email, double totalPurchase, String status) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.totalPurchase = totalPurchase;
        this.status = status;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public double getTotalPurchase() { return totalPurchase; }
    public String getStatus() { return status; }
}