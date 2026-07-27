package com.valliento.controller;

import com.valliento.db.ProductDAO;
import com.valliento.model.Product;
import com.valliento.session.Session;
import com.valliento.db.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ProductsController {

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, String> gstColumn;

    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private ComboBox<String> gstComboBox;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;

    // Labels shown in the dropdown, in order. "GST Exempt" maps to 0.0.
    private static final String GST_EXEMPT_LABEL = "GST Exempt";
    private static final ObservableList<String> GST_OPTIONS = FXCollections.observableArrayList(
        GST_EXEMPT_LABEL, "5%", "12%", "18%", "20%"
    );

    private static final ObservableList<String> CATEGORY_OPTIONS = FXCollections.observableArrayList(
        "Beverages", "Snacks", "Pastries", "Combo", "Others"
    );

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private Product selectedProduct = null;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));

        gstColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                rateToLabel(cellData.getValue().getGstRate())
            )
        );

        gstComboBox.setItems(GST_OPTIONS);
        gstComboBox.getSelectionModel().select(GST_EXEMPT_LABEL);

        categoryField.setItems(CATEGORY_OPTIONS);

        productsTable.setItems(products);

        productsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedProduct = newSel;
                nameField.setText(newSel.getName());
                categoryField.setValue(newSel.getCategory());
                priceField.setText(String.valueOf(newSel.getPrice()));
                stockField.setText(String.valueOf(newSel.getStock()));
                gstComboBox.getSelectionModel().select(rateToLabel(newSel.getGstRate()));
            }
        });

        loadProducts();
    }

    private void loadProducts() {
        products.setAll(ProductDAO.getAllProducts(currentLocationId()));
    }

    @FXML
    private void onAddProduct() {
        if (!validateForm()) return;

        boolean success = ProductDAO.addProduct(
            nameField.getText().trim(),
            categoryValue(),
            Double.parseDouble(priceField.getText().trim()),
            Integer.parseInt(stockField.getText().trim()),
            labelToRate(gstComboBox.getValue()),
            currentLocationId()
        );

        if (success) {
            loadProducts();
            clearForm();
        } else {
            showAlert(Alert.AlertType.ERROR, "Failed to add product.");
        }
    }

    @FXML
    private void onUpdateProduct() {
        if (selectedProduct == null) {
            showAlert(Alert.AlertType.WARNING, "Select a product from the table first.");
            return;
        }
        if (!validateForm()) return;

        boolean success = ProductDAO.updateProduct(
            selectedProduct.getId(),
            nameField.getText().trim(),
            categoryValue(),
            Double.parseDouble(priceField.getText().trim()),
            Integer.parseInt(stockField.getText().trim()),
            labelToRate(gstComboBox.getValue()),
            currentLocationId()
        );

        if (success) {
            loadProducts();
            clearForm();
        } else {
            showAlert(Alert.AlertType.ERROR, "Failed to update product.");
        }
    }

    @FXML
    private void onDeleteProduct() {
        if (selectedProduct == null) {
            showAlert(Alert.AlertType.WARNING, "Select a product from the table first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + selectedProduct.getName() + "\"? This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ProductDAO.deleteProduct(selectedProduct.getId(), currentLocationId());
                loadProducts();
                clearForm();
            }
        });
    }

    @FXML
    private void onClearForm() {
        clearForm();
    }

    private void clearForm() {
        nameField.clear();
        categoryField.getEditor().clear();
        categoryField.setValue(null);
        priceField.clear();
        stockField.clear();
        gstComboBox.getSelectionModel().select(GST_EXEMPT_LABEL);
        selectedProduct = null;
        productsTable.getSelectionModel().clearSelection();
    }

    private boolean validateForm() {
        if (nameField.getText().trim().isEmpty() || categoryValue().isEmpty()
            || priceField.getText().trim().isEmpty() || stockField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "All fields are required.");
            return false;
        }
        if (gstComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a GST rate (or GST Exempt).");
            return false;
        }
        try {
            Double.parseDouble(priceField.getText().trim());
            Integer.parseInt(stockField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Price must be a number and Stock must be a whole number.");
            return false;
        }
        return true;
    }

    private static String rateToLabel(double rate) {
        if (rate <= 0.0) return GST_EXEMPT_LABEL;
        if (rate == Math.floor(rate)) {
            return String.format("%.0f%%", rate);
        }
        return String.format("%s%%", rate);
    }

    private static double labelToRate(String label) {
        if (label == null || label.equals(GST_EXEMPT_LABEL)) return 0.0;
        return Double.parseDouble(label.replace("%", "").trim());
    }

    private String categoryValue() {
        String typed = categoryField.getEditor().getText();
        return typed == null ? "" : typed.trim();
    }

    private int currentLocationId() {
        return Session.getCurrentUser() != null ? Session.getCurrentUser().getLocationId() : DatabaseManager.DEFAULT_LOCATION_ID;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}