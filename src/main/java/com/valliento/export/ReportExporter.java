package com.valliento.export;

import com.valliento.model.Expense;
import com.valliento.model.SaleItemRecord;
import com.valliento.model.SaleRecord;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ReportExporter {

    public static void exportToExcel(List<SaleRecord> records, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sales Report");

            Row header = sheet.createRow(0);
            String[] cols = {"Invoice No", "Cashier", "Shift", "Total (Rs.)", "Date/Time"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            int rowNum = 1;
            double grandTotal = 0;
            for (SaleRecord r : records) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getInvoiceNo());
                row.createCell(1).setCellValue(r.getCashierName());
                row.createCell(2).setCellValue(r.getShift());
                row.createCell(3).setCellValue(r.getTotal());
                row.createCell(4).setCellValue(r.getCreatedAt());
                grandTotal += r.getTotal();
            }

            Row totalRow = sheet.createRow(rowNum + 1);
            totalRow.createCell(2).setCellValue("Grand Total:");
            totalRow.createCell(3).setCellValue(grandTotal);

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
        }
    }

    /**
     * Item-wise export: one row per product sold, with CGST/SGST/IGST columns
     * so the sheet is directly usable for GST reconciliation/filing.
     */
    public static void exportItemWiseToExcel(List<SaleItemRecord> records, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Item-wise Sales Report");

            Row header = sheet.createRow(0);
            String[] cols = {
                "Invoice No", "Product", "Qty", "Price (Rs.)", "Line Total (Rs.)",
                "GST %", "CGST (Rs.)", "SGST (Rs.)", "IGST (Rs.)", "Cashier", "Date/Time"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            int rowNum = 1;
            double grandTotal = 0;
            double totalCgst = 0;
            double totalSgst = 0;
            double totalIgst = 0;

            for (SaleItemRecord r : records) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getInvoiceNo());
                row.createCell(1).setCellValue(r.getProductName());
                row.createCell(2).setCellValue(r.getQty());
                row.createCell(3).setCellValue(r.getPrice());
                row.createCell(4).setCellValue(r.getLineTotal());
                row.createCell(5).setCellValue(r.getGstRate());
                row.createCell(6).setCellValue(r.getCgstAmount());
                row.createCell(7).setCellValue(r.getSgstAmount());
                row.createCell(8).setCellValue(r.getIgstAmount());
                row.createCell(9).setCellValue(r.getCashierName());
                row.createCell(10).setCellValue(r.getCreatedAt());

                grandTotal += r.getLineTotal();
                totalCgst += r.getCgstAmount();
                totalSgst += r.getSgstAmount();
                totalIgst += r.getIgstAmount();
            }

            Row totalRow = sheet.createRow(rowNum + 1);
            totalRow.createCell(3).setCellValue("Totals:");
            totalRow.createCell(4).setCellValue(grandTotal);
            totalRow.createCell(6).setCellValue(totalCgst);
            totalRow.createCell(7).setCellValue(totalSgst);
            totalRow.createCell(8).setCellValue(totalIgst);

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
        }
    }

    /**
     * Expense report export: one row per expense entry, with a grand total row.
     */
    public static void exportExpensesToExcel(List<Expense> records, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Expense Report");

            Row header = sheet.createRow(0);
            String[] cols = {"Expense Type", "Amount (Rs.)", "Note", "Date/Time"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            int rowNum = 1;
            double grandTotal = 0;
            for (Expense r : records) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getExpenseType());
                row.createCell(1).setCellValue(r.getAmount());
                row.createCell(2).setCellValue(r.getNote() == null ? "" : r.getNote());
                row.createCell(3).setCellValue(r.getCreatedAt());
                grandTotal += r.getAmount();
            }

            Row totalRow = sheet.createRow(rowNum + 1);
            totalRow.createCell(0).setCellValue("Grand Total:");
            totalRow.createCell(1).setCellValue(grandTotal);

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
        }
    }

    public static void exportToPdf(List<SaleRecord> records, String filePath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            PDType1Font boldFont = PDType1Font.HELVETICA_BOLD;
            PDType1Font regularFont = PDType1Font.HELVETICA;

            float y = 750;
            float margin = 50;

            content.setFont(boldFont, 16);
            content.beginText();
            content.newLineAtOffset(margin, y);
            content.showText("Valliento POS - Sales Report");
            content.endText();
            y -= 30;

            content.setFont(boldFont, 10);
            content.beginText();
            content.newLineAtOffset(margin, y);
            content.showText(String.format("%-15s %-15s %-8s %-10s %-20s", "Invoice", "Cashier", "Shift", "Total", "Date"));
            content.endText();
            y -= 18;

            content.setFont(regularFont, 9);
            double grandTotal = 0;

            for (SaleRecord r : records) {
                if (y < 60) {
                    content.close();
                    page = new PDPage();
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    content.setFont(regularFont, 9);
                    y = 750;
                }

                content.beginText();
                content.newLineAtOffset(margin, y);
                String line = String.format("%-15s %-15s %-8s %-10.2f %-20s",
                    r.getInvoiceNo(), r.getCashierName(), r.getShift(), r.getTotal(), r.getCreatedAt());
                content.showText(line);
                content.endText();
                y -= 15;
                grandTotal += r.getTotal();
            }

            y -= 10;
            content.setFont(boldFont, 11);
            content.beginText();
            content.newLineAtOffset(margin, y);
            content.showText(String.format("Grand Total: Rs.%.2f", grandTotal));
            content.endText();

            content.close();
            document.save(filePath);
        }
    }

    /** Item-wise PDF export, landscape-oriented since there are more columns to fit. */
    public static void exportItemWiseToPdf(List<SaleItemRecord> records, String filePath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDRectangle landscape = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            PDPage page = new PDPage(landscape);
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            PDType1Font boldFont = PDType1Font.HELVETICA_BOLD;
            PDType1Font regularFont = PDType1Font.HELVETICA;

            float y = landscape.getHeight() - 50;
            float margin = 40;

            content.setFont(boldFont, 16);
            content.beginText();
            content.newLineAtOffset(margin, y);
            content.showText("Valliento POS - Item-wise Sales Report (with GST)");
            content.endText();
            y -= 30;

            String headerFormat = "%-12s %-18s %-4s %-9s %-11s %-6s %-9s %-9s %-9s %-14s";
            content.setFont(boldFont, 8);
            content.beginText();
            content.newLineAtOffset(margin, y);
            content.showText(String.format(headerFormat,
                "Invoice", "Product", "Qty", "Price", "LineTotal", "GST%", "CGST", "SGST", "IGST", "Cashier"));
            content.endText();
            y -= 15;

            content.setFont(regularFont, 8);
            double grandTotal = 0, totalCgst = 0, totalSgst = 0, totalIgst = 0;

            for (SaleItemRecord r : records) {
                if (y < 50) {
                    content.close();
                    page = new PDPage(landscape);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    content.setFont(regularFont, 8);
                    y = landscape.getHeight() - 50;
                }

                content.beginText();
                content.newLineAtOffset(margin, y);
                String line = String.format(headerFormat,
                    r.getInvoiceNo(),
                    truncate(r.getProductName(), 18),
                    String.valueOf(r.getQty()),
                    String.format("%.2f", r.getPrice()),
                    String.format("%.2f", r.getLineTotal()),
                    String.format("%.1f", r.getGstRate()),
                    String.format("%.2f", r.getCgstAmount()),
                    String.format("%.2f", r.getSgstAmount()),
                    String.format("%.2f", r.getIgstAmount()),
                    truncate(r.getCashierName(), 14));
                content.showText(line);
                content.endText();
                y -= 13;

                grandTotal += r.getLineTotal();
                totalCgst += r.getCgstAmount();
                totalSgst += r.getSgstAmount();
                totalIgst += r.getIgstAmount();
            }

            y -= 10;
            content.setFont(boldFont, 10);
            content.beginText();
            content.newLineAtOffset(margin, y);
            content.showText(String.format("Grand Total: Rs.%.2f   CGST: Rs.%.2f   SGST: Rs.%.2f   IGST: Rs.%.2f",
                grandTotal, totalCgst, totalSgst, totalIgst));
            content.endText();

            content.close();
            document.save(filePath);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}