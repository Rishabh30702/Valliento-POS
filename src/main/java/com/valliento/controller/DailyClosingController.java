package com.valliento.controller;

import com.valliento.db.DailyClosingDAO;
import com.valliento.db.DatabaseManager;
import com.valliento.model.DailyClosing;
import com.valliento.session.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DailyClosingController {

    @FXML private DatePicker closingDatePicker;
    @FXML private Label totalSalesLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label netSalesLabel;
    @FXML private Label statusLabel;
    @FXML private Button closeDayButton;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @FXML
    public void initialize() {
        closingDatePicker.setValue(LocalDate.now());
        refresh();
    }

    private int currentLocationId() {
        return Session.getCurrentUser() != null ? Session.getCurrentUser().getLocationId() : DatabaseManager.DEFAULT_LOCATION_ID;
    }

    @FXML
    private void onDateChanged() {
        refresh();
    }

    private void refresh() {
        LocalDate date = closingDatePicker.getValue();
        if (date == null) return;
        String dateStr = date.format(FMT);
        int locationId = currentLocationId();

        DailyClosing existing = DailyClosingDAO.getClosingForDate(dateStr, locationId);

        double totalSales;
        int totalTransactions;
        double totalExpenses;
        double netSales;

        if (existing != null) {
            totalSales = existing.getTotalSales();
            totalTransactions = existing.getTotalTransactions();
            totalExpenses = existing.getTotalExpenses();
            netSales = existing.getNetSales();
            statusLabel.setText("Day is CLOSED (closed by " + existing.getClosedBy() + " at " + existing.getClosedAt() + ")");
            closeDayButton.setDisable(true);
        } else {
            totalSales = DailyClosingDAO.getSalesTotalForDate(dateStr, locationId);
            totalTransactions = DailyClosingDAO.getTransactionCountForDate(dateStr, locationId);
            totalExpenses = DailyClosingDAO.getExpensesTotalForDate(dateStr, locationId);
            netSales = totalSales - totalExpenses;
            statusLabel.setText("Day is OPEN");
            closeDayButton.setDisable(false);
        }

        totalSalesLabel.setText(String.format("\u20B9%,.2f", totalSales));
        totalTransactionsLabel.setText(String.valueOf(totalTransactions));
        totalExpensesLabel.setText(String.format("\u20B9%,.2f", totalExpenses));
        netSalesLabel.setText(String.format("\u20B9%,.2f", netSales));
    }

    @FXML
    private void onCloseDay() {
        LocalDate date = closingDatePicker.getValue();
        if (date == null) return;
        String dateStr = date.format(FMT);
        int locationId = currentLocationId();

        if (DailyClosingDAO.getClosingForDate(dateStr, locationId) != null) {
            showAlert("This day has already been closed.");
            refresh();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Close the day for " + dateStr + "? This cannot be undone.");
        confirm.setHeaderText(null);
        var result = confirm.showAndWait();
        if (result.isEmpty() || result.get().getButtonData().isCancelButton()) {
            return;
        }

        double totalSales = DailyClosingDAO.getSalesTotalForDate(dateStr, locationId);
        int totalTransactions = DailyClosingDAO.getTransactionCountForDate(dateStr, locationId);
        double totalExpenses = DailyClosingDAO.getExpensesTotalForDate(dateStr, locationId);
        double netSales = totalSales - totalExpenses;

        String closedBy = Session.getCurrentUser() != null ? Session.getCurrentUser().getFullName() : "Unknown";

        boolean success = DailyClosingDAO.closeDay(dateStr, totalSales, totalTransactions, totalExpenses, netSales, closedBy, locationId);
        if (success) {
            showAlert("Day closed successfully.");
            refresh();
        } else {
            showAlert("Failed to close the day. Please try again.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}