package com.valliento.model;

public class RestaurantTable {
    private int id;
    private String tableNo;
    private String status;

    public RestaurantTable(int id, String tableNo, String status) {
        this.id = id;
        this.tableNo = tableNo;
        this.status = status;
    }

    public int getId() { return id; }
    public String getTableNo() { return tableNo; }
    public String getStatus() { return status; }
}