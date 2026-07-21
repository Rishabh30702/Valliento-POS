package com.valliento.controller;

import com.valliento.db.ProductDAO;
import com.valliento.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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
    @FXML private TextField categoryField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private ComboBox<String> gstComboBox;

    // Labels shown in the dropdown, in order. "GST Exempt" maps to 0.0.
    private static final String GST_EXEMPT_LABEL = "GST Exempt";
    private static final ObservableList<String> GST_OPTIONS = FXCollections.observableArrayList(
        GST_EXEMPT_LABEL, "5%", "12%", "18%", "20%"
    );

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private Product selectedProduct = null;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // gst_rate is stored as a double (0.0 = exempt), so render it through
        // a value factory that converts it to a friendly label for the table.
        gstColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                rateToLabel(cellData.getValue().getGstRate())
            )
        );

        gstComboBox.setItems(GST_OPTIONS);
        gstComboBox.getSelectionModel().select(GST_EXEMPT_LABEL);

        productsTable.setItems(products);

        productsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedProduct = newSel;
                nameField.setText(newSel.getName());
                categoryField.setText(newSel.getCategory());
                priceField.setText(String.valueOf(newSel.getPrice()));
                stockField.setText(String.valueOf(newSel.getStock()));
                gstComboBox.getSelectionModel().select(rateToLabel(newSel.getGstRate()));
            }
        });

        loadProducts();
    }

    private void loadProducts() {
        products.setAll(ProductDAO.getAllProducts());
    }

    @FXML
    private void onAddProduct() {
        if (!validateForm()) return;

        boolean success = ProductDAO.addProduct(
            nameField.getText().trim(),
            categoryField.getText().trim(),
            Double.parseDouble(priceField.getText().trim()),
            Integer.parseInt(stockField.getText().trim()),
            labelToRate(gstComboBox.getValue())
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
            categoryField.getText().trim(),
            Double.parseDouble(priceField.getText().trim()),
            Integer.parseInt(stockField.getText().trim()),
            labelToRate(gstComboBox.getValue())
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
                ProductDAO.deleteProduct(selectedProduct.getId());
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
        categoryField.clear();
        priceField.clear();
        stockField.clear();
        gstComboBox.getSelectionModel().select(GST_EXEMPT_LABEL);
        selectedProduct = null;
        productsTable.getSelectionModel().clearSelection();
    }

    private boolean validateForm() {
        if (nameField.getText().trim().isEmpty() || categoryField.getText().trim().isEmpty()
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

    // Converts a stored gst_rate (0.0, 5.0, 12.0, 18.0, 20.0 ...) into the
    // label shown in the ComboBox / table column.
    private static String rateToLabel(double rate) {
        if (rate <= 0.0) return GST_EXEMPT_LABEL;
        if (rate == Math.floor(rate)) {
            return String.format("%.0f%%", rate);
        }
        return String.format("%s%%", rate);
    }

    // Converts the selected ComboBox label back into the numeric rate
    // expected by ProductDAO. "GST Exempt" -> 0.0.
    private static double labelToRate(String label) {
        if (label == null || label.equals(GST_EXEMPT_LABEL)) return 0.0;
        return Double.parseDouble(label.replace("%", "").trim());
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
