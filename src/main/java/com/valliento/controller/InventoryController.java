package com.valliento.controller;

import com.valliento.db.ProductDAO;
import com.valliento.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

public class InventoryController {

    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, String> statusColumn;

    @FXML private Label totalItemsLabel;
    @FXML private Label lowStockCountLabel;
    @FXML private TextField adjustAmountField;

    private final ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private Product selectedProduct = null;
    private static final int LOW_STOCK_THRESHOLD = 20;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));

        statusColumn.setCellValueFactory(data -> {
            int stock = data.getValue().getStock();
            String status = stock < LOW_STOCK_THRESHOLD ? "Low Stock" : "In Stock";
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTextFill(Color.BLACK);
                } else {
                    setText(item);
                    setTextFill(item.equals("Low Stock") ? Color.web("#c0392b") : Color.web("#2e7d32"));
                }
            }
        });

        inventoryTable.setItems(allProducts);

        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedProduct = newSel;
        });

        loadInventory();
    }

    private void loadInventory() {
        allProducts.setAll(ProductDAO.getAllProducts());
        totalItemsLabel.setText(String.valueOf(allProducts.size()));
        long lowStockCount = allProducts.stream().filter(p -> p.getStock() < LOW_STOCK_THRESHOLD).count();
        lowStockCountLabel.setText(String.valueOf(lowStockCount));
    }

    @FXML
    private void onAddStock() {
        adjustStock(true);
    }

    @FXML
    private void onRemoveStock() {
        adjustStock(false);
    }

    private void adjustStock(boolean isAdd) {
        if (selectedProduct == null) {
            showAlert("Select a product from the table first.");
            return;
        }
        String amountText = adjustAmountField.getText().trim();
        if (amountText.isEmpty()) {
            showAlert("Enter a quantity to adjust.");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(amountText);
        } catch (NumberFormatException e) {
            showAlert("Quantity must be a whole number.");
            return;
        }
        if (amount <= 0) {
            showAlert("Quantity must be greater than zero.");
            return;
        }

        int change = isAdd ? amount : -amount;
        if (!isAdd && selectedProduct.getStock() + change < 0) {
            showAlert("Cannot remove more than current stock (" + selectedProduct.getStock() + ").");
            return;
        }

        ProductDAO.adjustStock(selectedProduct.getId(), change);
        adjustAmountField.clear();
        loadInventory();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}