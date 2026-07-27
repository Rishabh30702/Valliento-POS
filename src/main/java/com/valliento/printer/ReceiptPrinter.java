package com.valliento.printer;

import com.valliento.model.CartItem;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.Paper;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReceiptPrinter {

    public static void printReceipt(String invoiceNo, String cashierName, List<CartItem> items,
                                     double subTotal, double tax, double grandTotal) {
        printReceipt(invoiceNo, cashierName, items, subTotal, tax, grandTotal, false, "Cash");
    }

    public static void printReceipt(String invoiceNo, String cashierName, List<CartItem> items,
                                     double subTotal, double tax, double grandTotal,
                                     boolean isInterState) {
        printReceipt(invoiceNo, cashierName, items, subTotal, tax, grandTotal, isInterState, "Cash");
    }

    public static void printReceipt(String invoiceNo, String cashierName, List<CartItem> items,
                                     double subTotal, double tax, double grandTotal,
                                     boolean isInterState, String paymentMethod) {

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showAlert(Alert.AlertType.ERROR, "No printer found. Please install a printer (thermal or regular) and try again.");
            return;
        }

        boolean proceed = job.showPrintDialog(null);
        if (!proceed) {
            return;
        }

        VBox receipt = buildReceiptLayout(invoiceNo, cashierName, items, subTotal, tax, grandTotal, isInterState, paymentMethod);

        receipt.setPrefWidth(226);

        boolean printed = job.printPage(receipt);
        if (printed) {
            job.endJob();
            showAlert(Alert.AlertType.INFORMATION, "Receipt sent to printer.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Failed to print receipt. Check the printer connection.");
        }
    }

    private static VBox buildReceiptLayout(String invoiceNo, String cashierName, List<CartItem> items,
                                            double subTotal, double tax, double grandTotal,
                                            boolean isInterState, String paymentMethod) {
        VBox box = new VBox(4);
        box.setStyle("-fx-padding: 10;");
        box.setAlignment(Pos.CENTER);

        Image logoImage = new Image(ReceiptPrinter.class.getResourceAsStream("/com/valliento/images/logo.png"));
        ImageView logo = new ImageView(logoImage);
        logo.setPreserveRatio(true);
        logo.setFitWidth(120);
        box.getChildren().add(logo);

        Text title = new Text("VALLIENTO POS");
        title.setFont(Font.font("Monospaced", 14));
        title.setTextAlignment(TextAlignment.CENTER);

        Text subtitle = new Text("Smart Billing. Seamless Business.");
        subtitle.setFont(Font.font("Monospaced", 8));

        Text invoiceLine = new Text("Invoice: " + invoiceNo);
        invoiceLine.setFont(Font.font("Monospaced", 9));

        Text dateLine = new Text("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        dateLine.setFont(Font.font("Monospaced", 9));

        Text cashierLine = new Text("Cashier: " + cashierName);
        cashierLine.setFont(Font.font("Monospaced", 9));

        Text saleTypeLine = new Text("Sale Type: " + (isInterState ? "Inter-State (IGST)" : "Intra-State (CGST+SGST)"));
        saleTypeLine.setFont(Font.font("Monospaced", 8));

        Text paymentLine = new Text("Payment: " + (paymentMethod == null || paymentMethod.isBlank() || paymentMethod.equals("---") ? "--" : paymentMethod));
        paymentLine.setFont(Font.font("Monospaced", 8));

        Text divider = new Text("--------------------------------");
        divider.setFont(Font.font("Monospaced", 9));

        box.getChildren().addAll(title, subtitle, invoiceLine,dateLine, cashierLine, saleTypeLine, paymentLine, divider);

        for (CartItem item : items) {
            String line = String.format("%-16s x%-2d %8.2f",
                truncate(item.getName(), 16), item.getQty(), item.getTotal());
            Text itemText = new Text(line);
            itemText.setFont(Font.font("Monospaced", 9));
            box.getChildren().add(itemText);
        }

        Text divider2 = new Text("--------------------------------");
        divider2.setFont(Font.font("Monospaced", 9));
        box.getChildren().add(divider2);

        Text subTotalLine = new Text(String.format("%-20s %10.2f", "Sub Total", subTotal));
        subTotalLine.setFont(Font.font("Monospaced", 9));
        box.getChildren().add(subTotalLine);

        // Sum taxable amount per rate bracket first (still needed for correct
        // per-item math), but instead of printing one CGST/SGST or IGST line
        // per rate bracket, we now accumulate everything into single totals
        // so the receipt always shows just one CGST line and one SGST line
        // (or one IGST line for inter-state), no matter how many different
        // GST rates the items on this bill actually have.
        Map<Double, Double> taxableAmountByRate = new TreeMap<>();
        for (CartItem item : items) {
            double rate = item.getGstRate();
            double lineTaxable = item.getTotal();
            taxableAmountByRate.merge(rate, lineTaxable, Double::sum);
        }

        double exemptAmt = 0.0;
        double totalGst = 0.0;
        double totalCgst = 0.0;
        double totalSgst = 0.0;
        double totalIgst = 0.0;

        for (Map.Entry<Double, Double> entry : taxableAmountByRate.entrySet()) {
            double rate = entry.getKey();
            double taxableAmt = entry.getValue();

            if (rate <= 0.0) {
                exemptAmt += taxableAmt;
                continue;
            }

            double gstAmt = taxableAmt * rate / 100.0;
            totalGst += gstAmt;

            if (isInterState) {
                totalIgst += gstAmt;
            } else {
                totalCgst += gstAmt / 2.0;
                totalSgst += gstAmt / 2.0;
            }
        }

        if (exemptAmt > 0.0) {
            Text exemptLine = new Text(String.format("%-20s %10.2f", "GST Exempt", exemptAmt));
            exemptLine.setFont(Font.font("Monospaced", 9));
            box.getChildren().add(exemptLine);
        }

        if (isInterState) {
            Text igstLine = new Text(String.format("%-20s %10.2f", "IGST", totalIgst));
            igstLine.setFont(Font.font("Monospaced", 9));
            box.getChildren().add(igstLine);
        } else {
            Text cgstLine = new Text(String.format("%-20s %10.2f", "CGST", totalCgst));
            cgstLine.setFont(Font.font("Monospaced", 9));
            box.getChildren().add(cgstLine);

            Text sgstLine = new Text(String.format("%-20s %10.2f", "SGST", totalSgst));
            sgstLine.setFont(Font.font("Monospaced", 9));
            box.getChildren().add(sgstLine);
        }

        Text totalGstLine = new Text(String.format("%-20s %10.2f", "Total GST", totalGst));
        totalGstLine.setFont(Font.font("Monospaced", 9));
        box.getChildren().add(totalGstLine);

        Text totalLine = new Text(String.format("%-20s %10.2f", "TOTAL", grandTotal));
        totalLine.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        box.getChildren().add(totalLine);

        Text thankYou = new Text("\nThank you for shopping with us!");
        thankYou.setFont(Font.font("Monospaced", 9));
        box.getChildren().add(thankYou);

        return box;
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}