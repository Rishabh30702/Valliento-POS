package com.valliento.controller;

import com.valliento.db.ProductDAO;
import com.valliento.db.SaleDAO;
import com.valliento.session.Session;
import com.valliento.db.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label todaysSalesLabel;
    @FXML private Label todaysTransactionsLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockLabel;

    @FXML
    public void initialize() {
        refreshStats();
    }

    private int currentLocationId() {
        return Session.getCurrentUser() != null ? Session.getCurrentUser().getLocationId() : DatabaseManager.DEFAULT_LOCATION_ID;
    }

    private void refreshStats() {
        double todaysSales = SaleDAO.getTodaysSalesTotal();
        int todaysTransactions = SaleDAO.getTodaysTransactionCount();
        int totalProducts = ProductDAO.getTotalProductCount(currentLocationId());
        int lowStock = ProductDAO.getLowStockCount(20, currentLocationId());

        todaysSalesLabel.setText(String.format("\u20B9%.2f", todaysSales));
        todaysTransactionsLabel.setText(todaysTransactions + " Transactions");
        totalProductsLabel.setText(String.valueOf(totalProducts));
        lowStockLabel.setText(lowStock + " Need Attention");
    }
}