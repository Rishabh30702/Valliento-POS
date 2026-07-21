package com.valliento.controller;

import com.valliento.db.ProductDAO;
import com.valliento.db.SaleDAO;
import com.valliento.model.CartItem;
import com.valliento.model.Product;
import com.valliento.session.Session;
import com.valliento.printer.ReceiptPrinter;
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
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

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

    // NEW: toggle between Intra-State (CGST+SGST) and Inter-State (IGST).
    // Add a <CheckBox fx:id="interStateCheckBox" text="Inter-State Sale (IGST)" />
    // to billing-content.fxml near the Table selector for this to bind.
    @FXML private CheckBox interStateCheckBox;

    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    // GST is now per-product (set on the Products page) instead of one flat rate here.

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

    private boolean isInterState() {
        return interStateCheckBox != null && interStateCheckBox.isSelected();
    }

    private void loadProductsFromDatabase() {
        productGrid.getChildren().clear();
        List<Product> products = ProductDAO.getAllProducts();
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

        if (isInterState()) {
            // Inter-state: full tax charged as IGST, no CGST/SGST split.
            cgstLabel.setText(String.format("IGST: \u20B9%.2f", tax));
            sgstLabel.setText("SGST: \u20B90.00");
        } else {
            // Intra-state: split the total tax evenly into CGST + SGST.
            double cgst = tax / 2;
            double sgst = tax / 2;
            cgstLabel.setText(String.format("CGST: \u20B9%.2f", cgst));
            sgstLabel.setText(String.format("SGST: \u20B9%.2f", sgst));
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
                List<Product> allProducts = ProductDAO.getAllProducts();
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

        RestaurantTable selectedTable = tableComboBox.getValue();
        Integer tableId = selectedTable != null ? selectedTable.getId() : null;
        String tableNo = selectedTable != null ? selectedTable.getTableNo() : "Takeaway";

        Map<String, Integer> items = new HashMap<>();
        for (CartItem item : cartItems) {
            items.put(item.getName(), item.getQty());
        }

        if (currentKotId != null) {
            boolean updated = KotDAO.replaceItemsForKot(currentKotId, items);
            if (!updated) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to update kitchen order.");
                alert.showAndWait();
                return;
            }
        } else {
            int kotId = KotDAO.createKot(tableId, tableNo, items);
            if (kotId == -1) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to send order to kitchen.");
                alert.showAndWait();
                return;
            }
            currentKotId = kotId;
        }

        if (selectedTable != null) {
            TableDAO.updateTableStatus(selectedTable.getId(), "Occupied");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Order sent to kitchen for " + tableNo + ".");
        alert.setHeaderText(null);
        alert.showAndWait();

        loadTables();
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

        double subTotal = cartItems.stream().mapToDouble(CartItem::getTotal).sum();
        double tax = cartItems.stream().mapToDouble(CartItem::getGstAmount).sum();
        double grandTotal = subTotal + tax;
        boolean interState = isInterState();

        String invoiceNo = SaleDAO.generateNextInvoiceNo();
        String cashierName = Session.getCurrentUser() != null ? Session.getCurrentUser().getFullName() : "Unknown";

        SaleDAO.recordSale(invoiceNo, grandTotal, tax, cartItems, cashierName, interState);

        ReceiptPrinter.printReceipt(invoiceNo, cashierName, cartItems, subTotal, tax, grandTotal, interState);

        RestaurantTable selectedTable = tableComboBox.getValue();
        if (selectedTable != null) {
            TableDAO.updateTableStatus(selectedTable.getId(), "Available");
        }
        if (currentKotId != null) {
            KotDAO.updateKotStatus(currentKotId, "Served");
        }

        cartItems.clear();
        currentKotId = null;
        suppressTableSelectionHandling = true;
        tableComboBox.setValue(null);
        suppressTableSelectionHandling = false;
        if (interStateCheckBox != null) {
            interStateCheckBox.setSelected(false);
        }
        updateTotals();
        loadTables();
    }
}