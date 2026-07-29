package com.valliento.controller;

import com.valliento.db.CategoryDAO;
import com.valliento.db.ProductDAO;
import com.valliento.model.Product;
import com.valliento.session.Session;
import com.valliento.db.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProductsController {

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, String> gstColumn;

    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryField;
    @FXML private Button addCategoryToggleButton;
    @FXML private VBox addCategoryPanel;
    @FXML private TextField newCategoryField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private ComboBox<String> gstComboBox;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private ProgressIndicator savingIndicator;

    private static final String GST_EXEMPT_LABEL = "GST Exempt";
    private static final ObservableList<String> GST_OPTIONS = FXCollections.observableArrayList(
        GST_EXEMPT_LABEL, "5%", "12%", "18%", "20%"
    );

    // Starting defaults - always shown even before any category has been
    // explicitly created for this location.
    private static final List<String> DEFAULT_CATEGORIES = List.of(
        "Beverages", "Snacks", "Pastries", "Combo", "Others"
    );

    private final ObservableList<String> categoryOptions = FXCollections.observableArrayList();
    private FilteredList<String> filteredCategoryOptions;

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

        filteredCategoryOptions = new FilteredList<>(categoryOptions, s -> true);
        categoryField.setItems(filteredCategoryOptions);
        setupCategorySearch();
        refreshCategories();

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

    /**
     * Search-as-you-type behavior for the Category ComboBox: typing narrows
     * the dropdown while always preserving exactly what the user typed, so a
     * brand-new category name is never silently discarded.
     */
    private void setupCategorySearch() {
        TextField editor = categoryField.getEditor();

        editor.textProperty().addListener((obs, oldText, newText) -> {
            String selected = categoryField.getSelectionModel().getSelectedItem();
            if (selected != null && selected.equals(newText)) {
                return;
            }

            if (newText == null || newText.isEmpty()) {
                filteredCategoryOptions.setPredicate(s -> true);
            } else {
                String lower = newText.toLowerCase();
                filteredCategoryOptions.setPredicate(s -> s.toLowerCase().contains(lower));
            }

            editor.setText(newText);
            editor.positionCaret(newText == null ? 0 : newText.length());

            if (!categoryField.isShowing() && editor.isFocused()) {
                categoryField.show();
            }
        });

        categoryField.setOnShowing(e -> {
            String current = editor.getText();
            if (current == null || current.isEmpty()) {
                filteredCategoryOptions.setPredicate(s -> true);
            }
        });
    }

    /**
     * Rebuilds the Category master list from defaults + explicitly created
     * categories (categories table) + anything already used by products.
     */
    private void refreshCategories() {
        Set<String> merged = new LinkedHashSet<>(DEFAULT_CATEGORIES);
        merged.addAll(CategoryDAO.getAllCategoryNames(currentLocationId()));
        categoryOptions.setAll(merged);
    }

    @FXML
    private void onToggleAddCategory() {
        boolean showing = addCategoryPanel.isVisible();
        addCategoryPanel.setVisible(!showing);
        addCategoryPanel.setManaged(!showing);
        if (!showing) {
            newCategoryField.clear();
            newCategoryField.requestFocus();
        }
    }

    @FXML
    private void onSaveNewCategory() {
        String name = newCategoryField.getText() == null ? "" : newCategoryField.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Enter a category name first.");
            return;
        }

        boolean added = CategoryDAO.addCategory(name, currentLocationId());
        if (!added) {
            showAlert(Alert.AlertType.WARNING, "That category already exists (or could not be added).");
        }

        refreshCategories();
        categoryField.setValue(name);
        newCategoryField.clear();
        addCategoryPanel.setVisible(false);
        addCategoryPanel.setManaged(false);
    }

    @FXML
    private void onCancelNewCategory() {
        newCategoryField.clear();
        addCategoryPanel.setVisible(false);
        addCategoryPanel.setManaged(false);
    }

    private void loadProducts() {
        products.setAll(ProductDAO.getAllProducts(currentLocationId()));
    }

    private void setBusy(boolean busy) {
        addButton.setDisable(busy);
        updateButton.setDisable(busy);
        deleteButton.setDisable(busy);
        clearButton.setDisable(busy);
        savingIndicator.setVisible(busy);
        savingIndicator.setManaged(busy);
    }

    @FXML
    private void onAddProduct() {
        if (!validateForm()) return;

        final String name = nameField.getText().trim();
        final String category = categoryValue();
        final double price = Double.parseDouble(priceField.getText().trim());
        final int stock = Integer.parseInt(stockField.getText().trim());
        final double gstRate = labelToRate(gstComboBox.getValue());
        final int locationId = currentLocationId();

        setBusy(true);
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return ProductDAO.addProduct(name, category, price, stock, gstRate, locationId);
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            if (task.getValue()) {
                loadProducts();
                refreshCategories();
                clearForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed to add product.");
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showAlert(Alert.AlertType.ERROR, "Failed to add product: " + task.getException().getMessage());
        });
        new Thread(task).start();
    }

    @FXML
    private void onUpdateProduct() {
        if (selectedProduct == null) {
            showAlert(Alert.AlertType.WARNING, "Select a product from the table first.");
            return;
        }
        if (!validateForm()) return;

        final int id = selectedProduct.getId();
        final String name = nameField.getText().trim();
        final String category = categoryValue();
        final double price = Double.parseDouble(priceField.getText().trim());
        final int stock = Integer.parseInt(stockField.getText().trim());
        final double gstRate = labelToRate(gstComboBox.getValue());
        final int locationId = currentLocationId();

        setBusy(true);
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return ProductDAO.updateProduct(id, name, category, price, stock, gstRate, locationId);
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            if (task.getValue()) {
                loadProducts();
                refreshCategories();
                clearForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed to update product.");
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showAlert(Alert.AlertType.ERROR, "Failed to update product: " + task.getException().getMessage());
        });
        new Thread(task).start();
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
                final int id = selectedProduct.getId();
                final int locationId = currentLocationId();

                setBusy(true);
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return ProductDAO.deleteProduct(id, locationId);
                    }
                };
                task.setOnSucceeded(e -> {
                    setBusy(false);
                    loadProducts();
                    refreshCategories();
                    clearForm();
                });
                task.setOnFailed(e -> {
                    setBusy(false);
                    showAlert(Alert.AlertType.ERROR, "Failed to delete product: " + task.getException().getMessage());
                });
                new Thread(task).start();
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
        addCategoryPanel.setVisible(false);
        addCategoryPanel.setManaged(false);
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