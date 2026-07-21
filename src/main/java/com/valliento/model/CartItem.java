package com.valliento.model;

import javafx.beans.property.*;

public class CartItem {
    private final StringProperty name;
    private final IntegerProperty qty;
    private final DoubleProperty price;
    private final DoubleProperty total;
    private final DoubleProperty gstRate;   // percentage, e.g. 5.0 = 5%, 0.0 = GST Exempt
    private final DoubleProperty gstAmount; // computed: total * (gstRate / 100)

    public CartItem(String name, int qty, double price, double gstRate) {
        this.name = new SimpleStringProperty(name);
        this.qty = new SimpleIntegerProperty(qty);
        this.price = new SimpleDoubleProperty(price);
        this.gstRate = new SimpleDoubleProperty(gstRate);
        this.total = new SimpleDoubleProperty(0);
        this.gstAmount = new SimpleDoubleProperty(0);
        recalcTotal();
    }

    // Backward-compatible overload for any existing callers that don't pass GST yet.
    public CartItem(String name, int qty, double price) {
        this(name, qty, price, 0.0);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public int getQty() { return qty.get(); }
    public void setQty(int q) { qty.set(q); recalcTotal(); }
    public IntegerProperty qtyProperty() { return qty; }

    public double getPrice() { return price.get(); }
    public DoubleProperty priceProperty() { return price; }

    public double getTotal() { return total.get(); }
    public DoubleProperty totalProperty() { return total; }

    public double getGstRate() { return gstRate.get(); }
    public DoubleProperty gstRateProperty() { return gstRate; }

    public double getGstAmount() { return gstAmount.get(); }
    public DoubleProperty gstAmountProperty() { return gstAmount; }

    private void recalcTotal() {
        total.set(qty.get() * price.get());
        gstAmount.set(total.get() * (gstRate.get() / 100.0));
    }
}