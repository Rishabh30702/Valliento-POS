package com.valliento.export;

import com.valliento.model.SaleRecord;
import org.apache.pdfbox.pdmodel.*;
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
            String[] cols = {"Invoice No", "Cashier", "Shift", "Total (₹)", "Date/Time"};
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
}