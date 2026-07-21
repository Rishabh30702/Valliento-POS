package com.valliento.model;

public class KotItem {
    private int id;
    private int kotId;
    private String productName;
    private int qty;

    public KotItem(int id, int kotId, String productName, int qty) {
        this.id = id;
        this.kotId = kotId;
        this.productName = productName;
        this.qty = qty;
    }

    public int getId() { return id; }
    public int getKotId() { return kotId; }
    public String getProductName() { return productName; }
    public int getQty() { return qty; }
}