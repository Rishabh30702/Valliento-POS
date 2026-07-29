package com.valliento.controller;

import com.valliento.db.ProductDAO;
import com.valliento.db.SaleDAO;
import com.valliento.session.Session;
import com.valliento.db.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.util.LinkedHashMap;

public class DashboardController {

    @FXML private Label todaysSalesLabel;
    @FXML private Label todaysTransactionsLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockLabel;
    @FXML private BarChart<String, Number> salesChart;
    @FXML private Label quoteLabel;
    @FXML private Label quoteAuthorLabel;

    private static final int MONTHS_TO_SHOW = 6;

    // A little personality for the dashboard - rotates daily, nothing to do
    // with sales/data, just a small morale boost for whoever's running the counter.
    private static final String[][] DAILY_QUOTES = {
        {"Hospitality is the art of making people feel at home, even when you kind of wish they'd go there.", "Unknown"},
        {"A good meal ought to begin with hunger.", "French Proverb"},
        {"The customer's perception is your reality.", "Kate Zabriskie"},
        {"Great service isn't about the transaction, it's about the interaction.", "Shep Hyken"},
        {"Every cup of coffee poured is a small act of hospitality.", "Unknown"},
        {"Take care of your customers and they will take care of your business.", "Unknown"},
        {"Small daily improvements are the key to staggering long-term results.", "Unknown"},
        {"A smile is the shortest distance between two people.", "Victor Borge"},
        {"Consistency is what transforms average into excellence.", "Unknown"},
        {"Well done is better than well said.", "Benjamin Franklin"}
    };

    @FXML
    public void initialize() {
        refreshStats();
        loadSalesChart();
        loadDailyQuote();
    }

    private int currentLocationId() {
        return Session.getCurrentUser() != null ? Session.getCurrentUser().getLocationId() : DatabaseManager.DEFAULT_LOCATION_ID;
    }

    private void refreshStats() {
        int locationId = currentLocationId();
        double todaysSales = SaleDAO.getTodaysSalesTotal(locationId);
        int todaysTransactions = SaleDAO.getTodaysTransactionCount(locationId);
        int totalProducts = ProductDAO.getTotalProductCount(locationId);
        int lowStock = ProductDAO.getLowStockCount(20, locationId);

        todaysSalesLabel.setText(String.format("\u20B9%.2f", todaysSales));
        todaysTransactionsLabel.setText(todaysTransactions + " Transactions");
        totalProductsLabel.setText(String.valueOf(totalProducts));
        lowStockLabel.setText(lowStock + " Need Attention");
    }

    private void loadSalesChart() {
        LinkedHashMap<String, Double> monthlyTotals = SaleDAO.getMonthlySalesTotals(MONTHS_TO_SHOW, currentLocationId());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Sales");
        for (var entry : monthlyTotals.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        salesChart.setData(FXCollections.observableArrayList(series));
    }

    private void loadDailyQuote() {
        int dayOfYear = LocalDate.now().getDayOfYear();
        String[] pick = DAILY_QUOTES[dayOfYear % DAILY_QUOTES.length];
        quoteLabel.setText("\u201C" + pick[0] + "\u201D");
        quoteAuthorLabel.setText("\u2014 " + pick[1]);
    }
}