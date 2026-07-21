package com.valliento.model;

public class SaleRecord {
    private final String invoiceNo;
    private final String cashierName;
    private final String shift;
    private final double total;
    private final String createdAt;

    public SaleRecord(String invoiceNo, String cashierName, String shift, double total, String createdAt) {
        this.invoiceNo = invoiceNo;
        this.cashierName = cashierName;
        this.shift = shift;
        this.total = total;
        this.createdAt = createdAt;
    }

    public String getInvoiceNo() { return invoiceNo; }
    public String getCashierName() { return cashierName; }
    public String getShift() { return shift; }
    public double getTotal() { return total; }
    public String getCreatedAt() { return createdAt; }
}