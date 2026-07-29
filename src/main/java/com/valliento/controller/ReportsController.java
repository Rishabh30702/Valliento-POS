package com.valliento.controller;

import com.valliento.db.DatabaseManager;
import com.valliento.db.SaleDAO;
import com.valliento.export.ReportExporter;
import com.valliento.model.SaleItemRecord;
import com.valliento.model.SaleRecord;
import com.valliento.session.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;

public class ReportsController {

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

    @FXML private RadioButton invoiceWiseRadio;
    @FXML private RadioButton itemWiseRadio;

    @FXML private TableView<SaleRecord> reportTable;
    @FXML private TableColumn<SaleRecord, String> invoiceColumn;
    @FXML private TableColumn<SaleRecord, String> cashierColumn;
    @FXML private TableColumn<SaleRecord, String> shiftColumn;
    @FXML private TableColumn<SaleRecord, Double> totalColumn;
    @FXML private TableColumn<SaleRecord, String> dateColumn;

    @FXML private TableView<SaleItemRecord> itemReportTable;
    @FXML private TableColumn<SaleItemRecord, String> itemInvoiceColumn;
    @FXML private TableColumn<SaleItemRecord, String> itemProductColumn;
    @FXML private TableColumn<SaleItemRecord, Integer> itemQtyColumn;
    @FXML private TableColumn<SaleItemRecord, Double> itemPriceColumn;
    @FXML private TableColumn<SaleItemRecord, Double> itemLineTotalColumn;
    @FXML private TableColumn<SaleItemRecord, Double> itemGstRateColumn;
    @FXML private TableColumn<SaleItemRecord, Double> itemCgstColumn;
    @FXML private TableColumn<SaleItemRecord, Double> itemSgstColumn;
    @FXML private TableColumn<SaleItemRecord, Double> itemIgstColumn;

    @FXML private Label grandTotalLabel;

    private final ObservableList<SaleRecord> records = FXCollections.observableArrayList();
    private final ObservableList<SaleItemRecord> itemRecords = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        invoiceColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
        cashierColumn.setCellValueFactory(new PropertyValueFactory<>("cashierName"));
        shiftColumn.setCellValueFactory(new PropertyValueFactory<>("shift"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        reportTable.setItems(records);

        itemInvoiceColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
        itemProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        itemQtyColumn.setCellValueFactory(new PropertyValueFactory<>("qty"));
        itemPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        itemLineTotalColumn.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        itemGstRateColumn.setCellValueFactory(new PropertyValueFactory<>("gstRate"));
        itemCgstColumn.setCellValueFactory(new PropertyValueFactory<>("cgstAmount"));
        itemSgstColumn.setCellValueFactory(new PropertyValueFactory<>("sgstAmount"));
        itemIgstColumn.setCellValueFactory(new PropertyValueFactory<>("igstAmount"));
        itemReportTable.setItems(itemRecords);

        fromDatePicker.setValue(LocalDate.now());
        toDatePicker.setValue(LocalDate.now());

        onFilter();
    }

    private int currentLocationId() {
        return Session.getCurrentUser() != null ? Session.getCurrentUser().getLocationId() : DatabaseManager.DEFAULT_LOCATION_ID;
    }

    private boolean isItemWiseMode() {
        return itemWiseRadio.isSelected();
    }

    @FXML
    private void onViewModeChanged() {
        boolean itemWise = isItemWiseMode();
        reportTable.setVisible(!itemWise);
        reportTable.setManaged(!itemWise);
        itemReportTable.setVisible(itemWise);
        itemReportTable.setManaged(itemWise);
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
        int locationId = currentLocationId();

        if (isItemWiseMode()) {
            itemRecords.setAll(SaleDAO.getItemWiseSalesReport(from, to, locationId));
            double grandTotal = itemRecords.stream().mapToDouble(SaleItemRecord::getLineTotal).sum();
            double totalGst = itemRecords.stream().mapToDouble(SaleItemRecord::getTotalGst).sum();
            grandTotalLabel.setText(String.format("Grand Total: \u20B9%.2f  |  Total GST: \u20B9%.2f  (%d line items)",
                grandTotal, totalGst, itemRecords.size()));
        } else {
            records.setAll(SaleDAO.getSalesReport(from, to, locationId));
            double grandTotal = records.stream().mapToDouble(SaleRecord::getTotal).sum();
            grandTotalLabel.setText(String.format("Grand Total: \u20B9%.2f  (%d sales)", grandTotal, records.size()));
        }
    }

    @FXML
    private void onExportExcel() {
        boolean itemWise = isItemWiseMode();
        if (itemWise ? itemRecords.isEmpty() : records.isEmpty()) {
            showAlert("No data to export. Adjust the date range first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Excel Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        chooser.setInitialFileName(itemWise ? "item-wise-sales-report.xlsx" : "sales-report.xlsx");
        Window window = reportTable.getScene().getWindow();
        File file = chooser.showSaveDialog(window);
        if (file == null) return;

        try {
            if (itemWise) {
                ReportExporter.exportItemWiseToExcel(itemRecords, file.getAbsolutePath());
            } else {
                ReportExporter.exportToExcel(records, file.getAbsolutePath());
            }
            showInfo("Excel report saved:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to export Excel: " + e.getMessage());
        }
    }

    @FXML
    private void onExportPdf() {
        boolean itemWise = isItemWiseMode();
        if (itemWise ? itemRecords.isEmpty() : records.isEmpty()) {
            showAlert("No data to export. Adjust the date range first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName(itemWise ? "item-wise-sales-report.pdf" : "sales-report.pdf");
        Window window = reportTable.getScene().getWindow();
        File file = chooser.showSaveDialog(window);
        if (file == null) return;

        try {
            if (itemWise) {
                ReportExporter.exportItemWiseToPdf(itemRecords, file.getAbsolutePath());
            } else {
                ReportExporter.exportToPdf(records, file.getAbsolutePath());
            }
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