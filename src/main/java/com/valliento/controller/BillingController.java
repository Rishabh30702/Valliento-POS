package com.valliento.controller;

import com.valliento.db.ProductDAO;
import com.valliento.db.SaleDAO;
import com.valliento.db.SettingsDAO;
import com.valliento.model.CartItem;
import com.valliento.model.Product;
import com.valliento.session.Session;
import com.valliento.printer.ReceiptPrinter;
import com.valliento.printer.UpiQrGenerator;
import com.valliento.db.KotDAO;
import com.valliento.db.TableDAO;
import com.valliento.model.KotItem;
import com.valliento.model.KotOrder;
import com.valliento.model.RestaurantTable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class BillingController {

    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> itemColumn;
    @FXML private TableColumn<CartItem, Integer> qtyColumn;
    @FXML private TableColumn<CartItem, Double> priceColumn;
    @FXML private TableColumn<CartItem, Double> totalColumn;
    @FXML private Label subTotalLabel;
    @FXML private Label cgstLabel;
    @FXML private Label sgstLabel;
    @FXML private Label totalLabel;
    @FXML private Label cashierLabel;
    @FXML private FlowPane productGrid;
    @FXML private ComboBox<RestaurantTable> tableComboBox;

    @FXML private CheckBox interStateCheckBox;
    @FXML private ComboBox<String> paymentMethodComboBox;

    @FXML private Button holdButton;
    @FXML private Button saveButton;
    @FXML private Button payButton;
    @FXML private Button sendToKitchenButton;
    @FXML private ProgressIndicator billingSpinner;

    private static final ObservableList<String> PAYMENT_METHODS =
        FXCollections.observableArrayList("---", "Cash", "Card", "UPI");

    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    private Integer currentKotId = null;
    private boolean suppressTableSelectionHandling = false;

    @FXML
    public void initialize() {
        itemColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("qty"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        cartTable.setItems(cartItems);

        if (Session.getCurrentUser() != null) {
            cashierLabel.setText("Cashier: " + Session.getCurrentUser().getFullName());
        } else {
            cashierLabel.setText("Cashier: Unknown");
        }

        if (paymentMethodComboBox != null) {
            paymentMethodComboBox.setItems(PAYMENT_METHODS);
            paymentMethodComboBox.getSelectionModel().select("---");
        }

        loadProductsFromDatabase();
        loadTables();
        updateTotals();

        if (interStateCheckBox != null) {
            interStateCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updateTotals());
        }

        tableComboBox.valueProperty().addListener((obs, oldTable, newTable) -> {
            if (suppressTableSelectionHandling) return;
            handleTableSelection(oldTable, newTable);
        });
    }

    private int currentLocationId() {
        return Session.getCurrentUser() != null ? Session.getCurrentUser().getLocationId() : com.valliento.db.DatabaseManager.DEFAULT_LOCATION_ID;
    }

    private void setBusy(boolean busy) {
        holdButton.setDisable(busy);
        saveButton.setDisable(busy);
        payButton.setDisable(busy);
        sendToKitchenButton.setDisable(busy);
        billingSpinner.setVisible(busy);
        billingSpinner.setManaged(busy);
    }

    private boolean isInterState() {
        return interStateCheckBox != null && interStateCheckBox.isSelected();
    }

    private String selectedPaymentMethod() {
        if (paymentMethodComboBox == null || paymentMethodComboBox.getValue() == null) {
            return "---";
        }
        return paymentMethodComboBox.getValue();
    }

    private void loadProductsFromDatabase() {
        productGrid.getChildren().clear();
        List<Product> products = ProductDAO.getAllProducts(currentLocationId());
        for (Product p : products) {
            VBox tile = new VBox(4);
            tile.getStyleClass().add("product-tile");
            tile.setPrefWidth(120);

            Label nameLabel = new Label(p.getName());
            nameLabel.getStyleClass().add("product-name");

            Label priceLabel = new Label(String.format("\u20B9%.2f", p.getPrice()));
            priceLabel.getStyleClass().add("product-price");

            tile.getChildren().addAll(nameLabel, priceLabel);
            tile.setOnMouseClicked(e -> addToCart(p.getName(), p.getPrice(), p.getGstRate()));

            productGrid.getChildren().add(tile);
        }
    }

    private void addToCart(String name, double price, double gstRate) {
        for (CartItem item : cartItems) {
            if (item.getName().equals(name)) {
                item.setQty(item.getQty() + 1);
                cartTable.refresh();
                updateTotals();
                return;
            }
        }
        cartItems.add(new CartItem(name, 1, price, gstRate));
        updateTotals();
    }

    private void updateTotals() {
        double subTotal = cartItems.stream().mapToDouble(CartItem::getTotal).sum();
        double tax = cartItems.stream().mapToDouble(CartItem::getGstAmount).sum();
        double grandTotal = subTotal + tax;

        subTotalLabel.setText(String.format("Sub Total: \u20B9%.2f", subTotal));

        double effectiveRate = subTotal > 0 ? (tax / subTotal) * 100.0 : 0.0;

        if (isInterState()) {
            cgstLabel.setText(String.format("IGST (%.1f%%): \u20B9%.2f", effectiveRate, tax));
            sgstLabel.setText("SGST: \u20B90.00");
        } else {
            double halfRate = effectiveRate / 2.0;
            double cgst = tax / 2;
            double sgst = tax / 2;
            cgstLabel.setText(String.format("CGST (%.1f%%): \u20B9%.2f", halfRate, cgst));
            sgstLabel.setText(String.format("SGST (%.1f%%): \u20B9%.2f", halfRate, sgst));
        }

        totalLabel.setText(String.format("Total: \u20B9%.2f", grandTotal));
    }

    private void loadTables() {
        RestaurantTable previousSelection = tableComboBox.getValue();
        suppressTableSelectionHandling = true;
        tableComboBox.getItems().setAll(TableDAO.getAllTables());
        tableComboBox.setConverter(new javafx.util.StringConverter<RestaurantTable>() {
            @Override
            public String toString(RestaurantTable t) {
                return t == null ? "Takeaway" : t.getTableNo() + " (" + t.getStatus() + ")";
            }
            @Override
            public RestaurantTable fromString(String s) {
                return null;
            }
        });
        if (previousSelection != null) {
            for (RestaurantTable t : tableComboBox.getItems()) {
                if (t.getId() == previousSelection.getId()) {
                    tableComboBox.setValue(t);
                    break;
                }
            }
        }
        suppressTableSelectionHandling = false;
    }

    private void handleTableSelection(RestaurantTable oldTable, RestaurantTable newTable) {
        boolean hasUnsentItems = !cartItems.isEmpty() && currentKotId == null;
        if (hasUnsentItems) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "You have items in the cart that haven't been sent to the kitchen yet. Switch tables and discard them?");
            confirm.setHeaderText(null);
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get().getButtonData().isCancelButton()) {
                suppressTableSelectionHandling = true;
                tableComboBox.setValue(oldTable);
                suppressTableSelectionHandling = false;
                return;
            }
        }

        cartItems.clear();
        currentKotId = null;

        if (newTable != null) {
            KotOrder activeKot = KotDAO.getActiveKotForTable(newTable.getId());
            if (activeKot != null) {
                currentKotId = activeKot.getId();
                List<KotItem> items = KotDAO.getItemsForKot(activeKot.getId());
                List<Product> allProducts = ProductDAO.getAllProducts(currentLocationId());
                for (KotItem item : items) {
                    double price = 0.0;
                    double gstRate = 5.0;
                    for (Product p : allProducts) {
                        if (p.getName().equals(item.getProductName())) {
                            price = p.getPrice();
                            gstRate = p.getGstRate();
                            break;
                        }
                    }
                    cartItems.add(new CartItem(item.getProductName(), item.getQty(), price, gstRate));
                }
            }
        }
        updateTotals();
    }

    @FXML
    private void onSendToKitchen() {
        if (cartItems.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Cart is empty!");
            alert.showAndWait();
            return;
        }

        final RestaurantTable selectedTable = tableComboBox.getValue();
        final Integer tableId = selectedTable != null ? selectedTable.getId() : null;
        final String tableNo = selectedTable != null ? selectedTable.getTableNo() : "Takeaway";
        final Integer finalCurrentKotId = currentKotId;

        final Map<String, Integer> items = new HashMap<>();
        for (CartItem item : cartItems) {
            items.put(item.getName(), item.getQty());
        }

        setBusy(true);
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                int resultKotId;
                if (finalCurrentKotId != null) {
                    boolean updated = KotDAO.replaceItemsForKot(finalCurrentKotId, items);
                    resultKotId = updated ? finalCurrentKotId : -1;
                } else {
                    resultKotId = KotDAO.createKot(tableId, tableNo, items);
                }
                if (resultKotId != -1 && selectedTable != null) {
                    TableDAO.updateTableStatus(selectedTable.getId(), "Occupied");
                }
                return resultKotId;
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            int resultKotId = task.getValue();
            if (resultKotId == -1) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to send order to kitchen.");
                alert.showAndWait();
                return;
            }
            currentKotId = resultKotId;
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Order sent to kitchen for " + tableNo + ".");
            alert.setHeaderText(null);
            alert.showAndWait();
            loadTables();
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to send order to kitchen: " + task.getException().getMessage());
            alert.showAndWait();
        });
        new Thread(task).start();
    }

    @FXML
    private void onHold() {
        if (cartItems.isEmpty()) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Order held (demo). Not yet saved to a holding queue.");
        alert.showAndWait();
    }

    @FXML
    private void onSave() {
        if (cartItems.isEmpty()) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Order saved as draft (demo).");
        alert.showAndWait();
    }

    @FXML
    private void onPayAndPrint() {
        if (cartItems.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Cart is empty!");
            alert.showAndWait();
            return;
        }

        final double subTotal = cartItems.stream().mapToDouble(CartItem::getTotal).sum();
        final double tax = cartItems.stream().mapToDouble(CartItem::getGstAmount).sum();
        final double grandTotal = subTotal + tax;
        final boolean interState = isInterState();
        final String paymentMethod = selectedPaymentMethod();
        final String invoiceNo = SaleDAO.generateNextInvoiceNo();

        if ("UPI".equals(paymentMethod)) {
            boolean confirmed = showUpiQrDialog(invoiceNo, grandTotal);
            if (!confirmed) {
                return;
            }
        }

        final String cashierName = Session.getCurrentUser() != null ? Session.getCurrentUser().getFullName() : "Unknown";
        final List<CartItem> itemsSnapshot = List.copyOf(cartItems);
        final RestaurantTable selectedTable = tableComboBox.getValue();
        final Integer finalCurrentKotId = currentKotId;

        setBusy(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                SaleDAO.recordSale(invoiceNo, grandTotal, tax, itemsSnapshot, cashierName, interState, paymentMethod);
                ReceiptPrinter.printReceipt(invoiceNo, cashierName, itemsSnapshot, subTotal, tax, grandTotal, interState, paymentMethod);
                if (selectedTable != null) {
                    TableDAO.updateTableStatus(selectedTable.getId(), "Available");
                }
                if (finalCurrentKotId != null) {
                    KotDAO.updateKotStatus(finalCurrentKotId, "Served");
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            cartItems.clear();
            currentKotId = null;
            suppressTableSelectionHandling = true;
            tableComboBox.setValue(null);
            suppressTableSelectionHandling = false;
            if (interStateCheckBox != null) {
                interStateCheckBox.setSelected(false);
            }
            if (paymentMethodComboBox != null) {
                paymentMethodComboBox.getSelectionModel().select("---");
            }
            updateTotals();
            loadTables();
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Payment failed: " + task.getException().getMessage());
            alert.showAndWait();
        });
        new Thread(task).start();
    }

    private boolean showUpiQrDialog(String invoiceNo, double amount) {
        String upiId = SettingsDAO.get("upi_id", "valliento.demo@upi");
        String merchantName = SettingsDAO.get("merchant_name", "Valliento POS");

        Image qrImage = UpiQrGenerator.generateUpiQr(upiId, merchantName, amount, "Invoice " + invoiceNo, 260);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Scan to Pay - UPI");

        VBox root = new VBox(12);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label heading = new Label("Scan with any UPI app to pay");
        heading.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(220);
        qrView.setFitHeight(220);

        Label amountLabel = new Label(String.format("Amount: \u20B9%.2f", amount));
        amountLabel.setStyle("-fx-font-size: 13px;");

        Label upiIdLabel = new Label("Paying to: " + upiId);
        upiIdLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        Button confirmBtn = new Button("Payment Received");
        confirmBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold;");

        Button cancelBtn = new Button("Cancel");

        final boolean[] result = {false};
        confirmBtn.setOnAction(e -> {
            result[0] = true;
            dialog.close();
        });
        cancelBtn.setOnAction(e -> {
            result[0] = false;
            dialog.close();
        });

        javafx.scene.layout.HBox buttonRow = new javafx.scene.layout.HBox(10, confirmBtn, cancelBtn);
        buttonRow.setAlignment(Pos.CENTER);

        root.getChildren().addAll(heading, qrView, amountLabel, upiIdLabel, buttonRow);

        dialog.setScene(new Scene(root, 320, 400));
        dialog.showAndWait();

        return result[0];
    }
}