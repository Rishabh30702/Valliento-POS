package com.valliento.controller;

import com.valliento.db.SaleDAO;
import com.valliento.export.ReportExporter;
import com.valliento.model.SaleRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class ReportsController {

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private TableView<SaleRecord> reportTable;
    @FXML private TableColumn<SaleRecord, String> invoiceColumn;
    @FXML private TableColumn<SaleRecord, String> cashierColumn;
    @FXML private TableColumn<SaleRecord, String> shiftColumn;
    @FXML private TableColumn<SaleRecord, Double> totalColumn;
    @FXML private TableColumn<SaleRecord, String> dateColumn;
    @FXML private Label grandTotalLabel;

    private final ObservableList<SaleRecord> records = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        invoiceColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
        cashierColumn.setCellValueFactory(new PropertyValueFactory<>("cashierName"));
        shiftColumn.setCellValueFactory(new PropertyValueFactory<>("shift"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        reportTable.setItems(records);

        fromDatePicker.setValue(LocalDate.now());
        toDatePicker.setValue(LocalDate.now());

        onFilter();
    }

    @FXML
    private void onFilter() {
        if (fromDatePicker.getValue() == null || toDatePicker.getValue() == null) {
            showAlert("Select both From and To dates.");
            return;
        }
        String from = fromDatePicker.getValue().toString();
        String to = toDatePicker.getValue().toString();
        records.setAll(SaleDAO.getSalesReport(from, to));

        double grandTotal = records.stream().mapToDouble(SaleRecord::getTotal).sum();
        grandTotalLabel.setText(String.format("Grand Total: \u20B9%.2f  (%d sales)", grandTotal, records.size()));
    }

    @FXML
    private void onExportExcel() {
        if (records.isEmpty()) {
            showAlert("No data to export. Adjust the date range first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Excel Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        chooser.setInitialFileName("sales-report.xlsx");
        Window window = reportTable.getScene().getWindow();
        File file = chooser.showSaveDialog(window);
        if (file == null) return;

        try {
            ReportExporter.exportToExcel(records, file.getAbsolutePath());
            showInfo("Excel report saved:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to export Excel: " + e.getMessage());
        }
    }

    @FXML
    private void onExportPdf() {
        if (records.isEmpty()) {
            showAlert("No data to export. Adjust the date range first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("sales-report.pdf");
        Window window = reportTable.getScene().getWindow();
        File file = chooser.showSaveDialog(window);
        if (file == null) return;

        try {
            ReportExporter.exportToPdf(records, file.getAbsolutePath());
            showInfo("PDF report saved:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to export PDF: " + e.getMessage());
        }
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