package com.valliento.controller;

import com.valliento.db.PurchaseDAO;
import com.valliento.db.SupplierDAO;
import com.valliento.model.PurchaseOrder;
import com.valliento.model.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PurchaseController {

    @FXML private TableView<PurchaseOrder> purchaseTable;
    @FXML private TableColumn<PurchaseOrder, String> invoiceColumn;
    @FXML private TableColumn<PurchaseOrder, String> supplierColumn;
    @FXML private TableColumn<PurchaseOrder, String> dateColumn;
    @FXML private TableColumn<PurchaseOrder, Double> amountColumn;
    @FXML private TableColumn<PurchaseOrder, String> statusColumn;

    @FXML private TextField invoiceField;
    @FXML private TextField invoiceSearchField;
    @FXML private ComboBox<Supplier> supplierComboBox;
    @FXML private DatePicker orderDatePicker;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> statusComboBox;

    private final ObservableList<PurchaseOrder> orderList = FXCollections.observableArrayList();
    private PurchaseOrder selectedOrder = null;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @FXML
    public void initialize() {
        invoiceColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        purchaseTable.setItems(orderList);

        statusComboBox.setItems(FXCollections.observableArrayList("Pending", "Completed", "Cancelled"));
        statusComboBox.setValue("Pending");

        supplierComboBox.setItems(FXCollections.observableArrayList(SupplierDAO.getAllSuppliers()));
        supplierComboBox.setConverter(new javafx.util.StringConverter<Supplier>() {
            @Override
            public String toString(Supplier supplier) {
                return supplier == null ? "" : supplier.getName();
            }
            @Override
            public Supplier fromString(String string) {
                return null;
            }
        });

        purchaseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                fillFormFromOrder(newSel);
            }
        });

        loadOrders();
        invoiceField.setText(PurchaseDAO.generateNextInvoiceNo());
    }

    /** Populates the form with an existing order's data, for editing (via row click or invoice search). */
    private void fillFormFromOrder(PurchaseOrder order) {
        selectedOrder = order;
        invoiceField.setText(order.getInvoiceNo());
        for (Supplier s : supplierComboBox.getItems()) {
            if (s.getId() == order.getSupplierId()) {
                supplierComboBox.setValue(s);
                break;
            }
        }
        orderDatePicker.setValue(LocalDate.parse(order.getOrderDate(), FMT));
        amountField.setText(String.valueOf(order.getAmount()));
        statusComboBox.setValue(order.getStatus());
    }

    private void loadOrders() {
        orderList.setAll(PurchaseDAO.getAllPurchaseOrders());
    }

    @FXML
    private void onFindByInvoice() {
        String invoiceNo = invoiceSearchField.getText().trim();
        if (invoiceNo.isEmpty()) {
            showAlert("Enter an invoice number to search for.");
            return;
        }

        PurchaseOrder found = PurchaseDAO.findByInvoiceNo(invoiceNo);
        if (found == null) {
            showAlert("No purchase order found with invoice number \"" + invoiceNo + "\".");
            return;
        }

        fillFormFromOrder(found);

        // Highlight the matching row in the table too
        for (PurchaseOrder o : orderList) {
            if (o.getId() == found.getId()) {
                purchaseTable.getSelectionModel().select(o);
                purchaseTable.scrollTo(o);
                break;
            }
        }
    }

    @FXML
    private void onAddOrder() {
        if (!validateForm()) return;

        PurchaseDAO.addPurchaseOrder(
            invoiceField.getText().trim(),
            supplierComboBox.getValue().getId(),
            supplierComboBox.getValue().getName(),
            orderDatePicker.getValue().format(FMT),
            Double.parseDouble(amountField.getText().trim()),
            statusComboBox.getValue()
        );
        clearForm();
        loadOrders();
    }

    @FXML
    private void onUpdateOrder() {
        if (selectedOrder == null) {
            showAlert("Select an order from the table first.");
            return;
        }
        if (!validateForm()) return;

        PurchaseDAO.updatePurchaseOrder(
            selectedOrder.getId(),
            invoiceField.getText().trim(),
            supplierComboBox.getValue().getId(),
            supplierComboBox.getValue().getName(),
            orderDatePicker.getValue().format(FMT),
            Double.parseDouble(amountField.getText().trim()),
            statusComboBox.getValue()
        );
        clearForm();
        loadOrders();
    }

    @FXML
    private void onDeleteOrder() {
        if (selectedOrder == null) {
            showAlert("Select an order from the table first.");
            return;
        }
        PurchaseDAO.deletePurchaseOrder(selectedOrder.getId());
        clearForm();
        loadOrders();
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private boolean validateForm() {
        if (invoiceField.getText().trim().isEmpty()) {
            showAlert("Invoice number is required.");
            return false;
        }
        if (supplierComboBox.getValue() == null) {
            showAlert("Select a supplier.");
            return false;
        }
        if (orderDatePicker.getValue() == null) {
            showAlert("Select an order date.");
            return false;
        }
        String amountText = amountField.getText().trim();
        if (amountText.isEmpty()) {
            showAlert("Enter an amount.");
            return false;
        }
        try {
            double amt = Double.parseDouble(amountText);
            if (amt <= 0) {
                showAlert("Amount must be greater than zero.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Amount must be a valid number.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        invoiceSearchField.clear();
        supplierComboBox.setValue(null);
        orderDatePicker.setValue(null);
        amountField.clear();
        statusComboBox.setValue("Pending");
        selectedOrder = null;
        purchaseTable.getSelectionModel().clearSelection();
        invoiceField.setText(PurchaseDAO.generateNextInvoiceNo());
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}