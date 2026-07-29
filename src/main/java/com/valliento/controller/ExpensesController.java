package com.valliento.controller;

import com.valliento.db.ExpenseDAO;
import com.valliento.export.ReportExporter;
import com.valliento.model.Expense;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;

public class ExpensesController {

    @FXML private TableView<Expense> expensesTable;
    @FXML private TableColumn<Expense, String> typeColumn;
    @FXML private TableColumn<Expense, Double> amountColumn;
    @FXML private TableColumn<Expense, String> noteColumn;
    @FXML private TableColumn<Expense, String> dateColumn;

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

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
            if (newSel != null) {
                typeField.setText(newSel.getExpenseType());
                amountField.setText(String.valueOf(newSel.getAmount()));
                noteField.setText(newSel.getNote());
            }
        });

        fromDatePicker.setValue(LocalDate.now());
        toDatePicker.setValue(LocalDate.now());

        loadExpenses();
    }

    private void loadExpenses() {
        expenseList.setAll(ExpenseDAO.getAllExpenses());
        refreshTodaysTotal();
    }

    private void refreshTodaysTotal() {
        todaysTotalLabel.setText(String.format("Today's Expenses: \u20B9%.2f", ExpenseDAO.getTodaysExpenseTotal()));
    }

    @FXML
    private void onFilter() {
        if (fromDatePicker.getValue() == null || toDatePicker.getValue() == null) {
            showAlert("Select both From and To dates.");
            return;
        }
        String from = fromDatePicker.getValue().toString();
        String to = toDatePicker.getValue().toString();
        expenseList.setAll(ExpenseDAO.getExpensesByDateRange(from, to));
        refreshTodaysTotal();
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
    private void onUpdateExpense() {
        if (selectedExpense == null) {
            showAlert("Select an expense from the table first.");
            return;
        }

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

        ExpenseDAO.updateExpense(selectedExpense.getId(), type, amount, noteField.getText().trim());
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
    private void onExportExcel() {
        if (expenseList.isEmpty()) {
            showAlert("No data to export. Adjust the date range first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Excel Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        chooser.setInitialFileName("expense-report.xlsx");
        Window window = expensesTable.getScene().getWindow();
        File file = chooser.showSaveDialog(window);
        if (file == null) return;

        try {
            ReportExporter.exportExpensesToExcel(expenseList, file.getAbsolutePath());
            showInfo("Excel report saved:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to export Excel: " + e.getMessage());
        }
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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}