package com.valliento.model;

public class DailyClosing {
    private int id;
    private String closingDate;
    private double totalSales;
    private int totalTransactions;
    private double totalExpenses;
    private double netSales;
    private String closedBy;
    private String closedAt;

    public DailyClosing(int id, String closingDate, double totalSales, int totalTransactions,
                         double totalExpenses, double netSales, String closedBy, String closedAt) {
        this.id = id;
        this.closingDate = closingDate;
        this.totalSales = totalSales;
        this.totalTransactions = totalTransactions;
        this.totalExpenses = totalExpenses;
        this.netSales = netSales;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
    }

    public int getId() { return id; }
    public String getClosingDate() { return closingDate; }
    public double getTotalSales() { return totalSales; }
    public int getTotalTransactions() { return totalTransactions; }
    public double getTotalExpenses() { return totalExpenses; }
    public double getNetSales() { return netSales; }
    public String getClosedBy() { return closedBy; }
    public String getClosedAt() { return closedAt; }
}