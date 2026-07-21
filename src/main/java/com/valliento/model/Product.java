package com.valliento.model;

import javafx.beans.property.*;

public class Product {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty category;
    private final DoubleProperty price;
    private final IntegerProperty stock;
    private final DoubleProperty gstRate; // stored as a percentage, e.g. 5.0 = 5%, 0.0 = GST Exempt

    public Product(int id, String name, String category, double price, int stock, double gstRate) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.category = new SimpleStringProperty(category);
        this.price = new SimpleDoubleProperty(price);
        this.stock = new SimpleIntegerProperty(stock);
        this.gstRate = new SimpleDoubleProperty(gstRate);
    }

    // Backward-compatible overload for any existing callers that don't pass GST yet.
    // Defaults to 0.0 (GST Exempt) so nothing else in the codebase breaks.
    public Product(int id, String name, String category, double price, int stock) {
        this(id, name, category, price, stock, 0.0);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public StringProperty nameProperty() { return name; }

    public String getCategory() { return category.get(); }
    public void setCategory(String v) { category.set(v); }
    public StringProperty categoryProperty() { return category; }

    public double getPrice() { return price.get(); }
    public void setPrice(double v) { price.set(v); }
    public DoubleProperty priceProperty() { return price; }

    public int getStock() { return stock.get(); }
    public void setStock(int v) { stock.set(v); }
    public IntegerProperty stockProperty() { return stock; }

    public double getGstRate() { return gstRate.get(); }
    public void setGstRate(double v) { gstRate.set(v); }
    public DoubleProperty gstRateProperty() { return gstRate; }
}