package com.valliento.controller;

import com.valliento.db.ReportDAO;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ReportController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private Label totalSalesLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label averageBillLabel;

    @FXML private BarChart<String, Number> salesChart;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.minusDays(13));
        endDatePicker.setValue(today);
        onFilter();
    }

    @FXML
    private void onFilter() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start == null || end == null || start.isAfter(end)) {
            return;
        }
        String startStr = start.format(FMT);
        String endStr = end.format(FMT);

        double totalSales = ReportDAO.getTotalSales(startStr, endStr);
        int transactionCount = ReportDAO.getTransactionCount(startStr, endStr);
        double avgBill = transactionCount == 0 ? 0.0 : totalSales / transactionCount;

        totalSalesLabel.setText(String.format("₹%,.2f", totalSales));
        totalTransactionsLabel.setText(String.valueOf(transactionCount));
        averageBillLabel.setText(String.format("₹%,.2f", avgBill));

        Map<String, Double> daily = ReportDAO.getDailySales(startStr, endStr);
        salesChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Sales");
        for (Map.Entry<String, Double> entry : daily.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        salesChart.getData().add(series);
    }
}