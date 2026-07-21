package com.valliento.controller;

import com.valliento.db.ProductDAO;
import com.valliento.db.SaleDAO;
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

    private void refreshStats() {
        double todaysSales = SaleDAO.getTodaysSalesTotal();
        int todaysTransactions = SaleDAO.getTodaysTransactionCount();
        int totalProducts = ProductDAO.getTotalProductCount();
        int lowStock = ProductDAO.getLowStockCount(20);

        todaysSalesLabel.setText(String.format("\u20B9%.2f", todaysSales));
        todaysTransactionsLabel.setText(todaysTransactions + " Transactions");
        totalProductsLabel.setText(String.valueOf(totalProducts));
        lowStockLabel.setText(lowStock + " Need Attention");
    }
}