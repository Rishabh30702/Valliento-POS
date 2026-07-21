package com.valliento.model;

public class Expense {
    private int id;
    private String expenseType;
    private double amount;
    private String note;
    private String createdAt;

    public Expense(int id, String expenseType, double amount, String note, String createdAt) {
        this.id = id;
        this.expenseType = expenseType;
        this.amount = amount;
        this.note = note;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getExpenseType() { return expenseType; }
    public double getAmount() { return amount; }
    public String getNote() { return note; }
    public String getCreatedAt() { return createdAt; }
}