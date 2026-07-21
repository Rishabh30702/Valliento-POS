package com.valliento.controller;

import com.valliento.db.ExpenseDAO;
import com.valliento.model.Expense;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ExpensesController {

    @FXML private TableView<Expense> expensesTable;
    @FXML private TableColumn<Expense, String> typeColumn;
    @FXML private TableColumn<Expense, Double> amountColumn;
    @FXML private TableColumn<Expense, String> noteColumn;
    @FXML private TableColumn<Expense, String> dateColumn;

    @FXML private TextField typeField;
    @FXML private TextField amountField;
    @FXML private TextField noteField;
    @FXML private Label todaysTotalLabel;

    private final ObservableList<Expense> expenseList = FXCollections.observableArrayList();
    private Expense selectedExpense = null;

    @FXML
    public void initialize() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("expenseType"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        expensesTable.setItems(expenseList);

        expensesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedExpense = newSel;
        });

        loadExpenses();
    }

    private void loadExpenses() {
        expenseList.setAll(ExpenseDAO.getAllExpenses());
        todaysTotalLabel.setText(String.format("Today's Expenses: \u20B9%.2f", ExpenseDAO.getTodaysExpenseTotal()));
    }

    @FXML
    private void onAddExpense() {
        String type = typeField.getText().trim();
        String amountText = amountField.getText().trim();

        if (type.isEmpty() || amountText.isEmpty()) {
            showAlert("Expense type and amount are required.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            showAlert("Amount must be a valid number.");
            return;
        }

        ExpenseDAO.addExpense(type, amount, noteField.getText().trim());
        clearForm();
        loadExpenses();
    }

    @FXML
    private void onDeleteExpense() {
        if (selectedExpense == null) {
            showAlert("Select an expense from the table first.");
            return;
        }
        ExpenseDAO.deleteExpense(selectedExpense.getId());
        clearForm();
        loadExpenses();
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private void clearForm() {
        typeField.clear();
        amountField.clear();
        noteField.clear();
        selectedExpense = null;
        expensesTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}