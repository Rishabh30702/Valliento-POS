package com.valliento.controller;

import com.valliento.db.SupplierDAO;
import com.valliento.model.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class SuppliersController {

    @FXML private TableView<Supplier> suppliersTable;
    @FXML private TableColumn<Supplier, String> nameColumn;
    @FXML private TableColumn<Supplier, String> phoneColumn;
    @FXML private TableColumn<Supplier, String> emailColumn;
    @FXML private TableColumn<Supplier, Double> totalPurchaseColumn;
    @FXML private TableColumn<Supplier, String> statusColumn;

    @FXML private TextField searchField;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;

    private final ObservableList<Supplier> supplierList = FXCollections.observableArrayList();
    private Supplier selectedSupplier = null;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        totalPurchaseColumn.setCellValueFactory(new PropertyValueFactory<>("totalPurchase"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        suppliersTable.setItems(supplierList);

        suppliersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedSupplier = newSel;
            if (newSel != null) {
                nameField.setText(newSel.getName());
                phoneField.setText(newSel.getPhone());
                emailField.setText(newSel.getEmail());
            }
        });

        loadSuppliers();
    }

    private void loadSuppliers() {
        supplierList.setAll(SupplierDAO.getAllSuppliers());
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();
        supplierList.setAll(query.isEmpty() ? SupplierDAO.getAllSuppliers() : SupplierDAO.searchSuppliers(query));
    }

    @FXML
    private void onAddSupplier() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Supplier name is required.");
            return;
        }
        SupplierDAO.addSupplier(name, phoneField.getText().trim(), emailField.getText().trim());
        clearForm();
        loadSuppliers();
    }

    @FXML
    private void onUpdateSupplier() {
        if (selectedSupplier == null) {
            showAlert("Select a supplier from the table first.");
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Supplier name is required.");
            return;
        }
        SupplierDAO.updateSupplier(selectedSupplier.getId(), name, phoneField.getText().trim(), emailField.getText().trim());
        clearForm();
        loadSuppliers();
    }

    @FXML
    private void onDeleteSupplier() {
        if (selectedSupplier == null) {
            showAlert("Select a supplier from the table first.");
            return;
        }
        SupplierDAO.deleteSupplier(selectedSupplier.getId());
        clearForm();
        loadSuppliers();
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private void clearForm() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        selectedSupplier = null;
        suppliersTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}