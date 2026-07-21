package com.valliento.model;

public class KotOrder {
    private int id;
    private String kotNo;
    private Integer tableId;
    private String tableNo;
    private String status;
    private String createdAt;

    public KotOrder(int id, String kotNo, Integer tableId, String tableNo, String status, String createdAt) {
        this.id = id;
        this.kotNo = kotNo;
        this.tableId = tableId;
        this.tableNo = tableNo;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getKotNo() { return kotNo; }
    public Integer getTableId() { return tableId; }
    public String getTableNo() { return tableNo; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}