package com.valliento.model;

public class PurchaseOrder {
    private int id;
    private String invoiceNo;
    private int supplierId;
    private String supplierName;
    private String orderDate;
    private double amount;
    private String status;

    public PurchaseOrder(int id, String invoiceNo, int supplierId, String supplierName,
                          String orderDate, double amount, String status) {
        this.id = id;
        this.invoiceNo = invoiceNo;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.orderDate = orderDate;
        this.amount = amount;
        this.status = status;
    }

    public int getId() { return id; }
    public String getInvoiceNo() { return invoiceNo; }
    public int getSupplierId() { return supplierId; }
    public String getSupplierName() { return supplierName; }
    public String getOrderDate() { return orderDate; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
}