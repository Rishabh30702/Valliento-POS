package com.valliento.controller;

import com.valliento.db.CustomerDAO;
import com.valliento.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class CustomerController {

    @FXML private TableView<Customer> customersTable;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, Double> totalPurchaseColumn;
    @FXML private TableColumn<Customer, Integer> pointsColumn;
    @FXML private TableColumn<Customer, String> statusColumn;

    @FXML private TextField searchField;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;

    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private Customer selectedCustomer = null;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        totalPurchaseColumn.setCellValueFactory(new PropertyValueFactory<>("totalPurchase"));
        pointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        customersTable.setItems(customerList);

        customersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedCustomer = newSel;
            if (newSel != null) {
                nameField.setText(newSel.getName());
                phoneField.setText(newSel.getPhone());
                emailField.setText(newSel.getEmail());
            }
        });

        loadCustomers();
    }

    private void loadCustomers() {
        customerList.setAll(CustomerDAO.getAllCustomers());
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();
        customerList.setAll(query.isEmpty() ? CustomerDAO.getAllCustomers() : CustomerDAO.searchCustomers(query));
    }

    @FXML
    private void onAddCustomer() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Customer name is required.");
            return;
        }
        CustomerDAO.addCustomer(name, phoneField.getText().trim(), emailField.getText().trim());
        clearForm();
        loadCustomers();
    }

    @FXML
    private void onUpdateCustomer() {
        if (selectedCustomer == null) {
            showAlert("Select a customer from the table first.");
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Customer name is required.");
            return;
        }
        CustomerDAO.updateCustomer(selectedCustomer.getId(), name, phoneField.getText().trim(), emailField.getText().trim());
        clearForm();
        loadCustomers();
    }

    @FXML
    private void onDeleteCustomer() {
        if (selectedCustomer == null) {
            showAlert("Select a customer from the table first.");
            return;
        }
        CustomerDAO.deleteCustomer(selectedCustomer.getId());
        clearForm();
        loadCustomers();
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private void clearForm() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        selectedCustomer = null;
        customersTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}