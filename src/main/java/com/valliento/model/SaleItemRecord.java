package com.valliento.model;

public class SaleItemRecord {
    private final String invoiceNo;
    private final String productName;
    private final int qty;
    private final double price;
    private final double lineTotal;
    private final double gstRate;
    private final double cgstAmount;
    private final double sgstAmount;
    private final double igstAmount;
    private final String cashierName;
    private final String createdAt;

    public SaleItemRecord(String invoiceNo, String productName, int qty, double price,
                           double gstRate, double cgstAmount, double sgstAmount, double igstAmount,
                           String cashierName, String createdAt) {
        this.invoiceNo = invoiceNo;
        this.productName = productName;
        this.qty = qty;
        this.price = price;
        this.lineTotal = qty * price;
        this.gstRate = gstRate;
        this.cgstAmount = cgstAmount;
        this.sgstAmount = sgstAmount;
        this.igstAmount = igstAmount;
        this.cashierName = cashierName;
        this.createdAt = createdAt;
    }

    public String getInvoiceNo() { return invoiceNo; }
    public String getProductName() { return productName; }
    public int getQty() { return qty; }
    public double getPrice() { return price; }
    public double getLineTotal() { return lineTotal; }
    public double getGstRate() { return gstRate; }
    public double getCgstAmount() { return cgstAmount; }
    public double getSgstAmount() { return sgstAmount; }
    public double getIgstAmount() { return igstAmount; }
    public String getCashierName() { return cashierName; }
    public String getCreatedAt() { return createdAt; }

    public double getTotalGst() { return cgstAmount + sgstAmount + igstAmount; }
}