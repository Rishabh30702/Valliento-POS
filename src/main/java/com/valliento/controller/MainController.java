package com.valliento.controller;

import com.valliento.session.RolePermissions;
import com.valliento.session.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private Button navDashboard;
    @FXML private Button navBilling;
    @FXML private Button navProducts;
    @FXML private Button navInventory;
    @FXML private Button navDailyClosing;
    @FXML private Button navTableManagement;
    @FXML private Button navRoomManagement;
    @FXML private Button navKot;
    @FXML private Button navPurchase;
    @FXML private Button navCustomers;
    @FXML private Button navSuppliers;
    @FXML private Button navExpenses;
    @FXML private Button navEmployees;
    @FXML private Button navReports;
    @FXML private Button navSettings;
    @FXML private Label loggedInUserLabel;
    @FXML private StackPane contentStack;
    @FXML private VBox loadingOverlay;

    private Map<String, Button> moduleButtons;
    private Map<String, String> moduleFxml;

    @FXML
    public void initialize() {
        moduleButtons = new LinkedHashMap<>();
        moduleButtons.put("dashboard", navDashboard);
        moduleButtons.put("billing", navBilling);
        moduleButtons.put("products", navProducts);
        moduleButtons.put("inventory", navInventory);
        moduleButtons.put("purchase", navPurchase);
        moduleButtons.put("tableManagement", navTableManagement);
        moduleButtons.put("roomManagement", navRoomManagement);
        moduleButtons.put("kot", navKot);
        moduleButtons.put("customers", navCustomers);
        moduleButtons.put("suppliers", navSuppliers);
        moduleButtons.put("expenses", navExpenses);
        moduleButtons.put("employees", navEmployees);
        moduleButtons.put("reports", navReports);
        moduleButtons.put("dailyClosing", navDailyClosing);
        moduleButtons.put("settings", navSettings);

        moduleFxml = new LinkedHashMap<>();
        moduleFxml.put("dashboard", "dashboard-content.fxml");
        moduleFxml.put("billing", "billing-content.fxml");
        moduleFxml.put("products", "products-content.fxml");
        moduleFxml.put("inventory", "inventory-content.fxml");
        moduleFxml.put("purchase", "purchase-content.fxml");
        moduleFxml.put("tableManagement", "table-content.fxml");
        moduleFxml.put("roomManagement", "room-content.fxml");
        moduleFxml.put("kot", "kot-content.fxml");
        moduleFxml.put("customers", "customers-content.fxml");
        moduleFxml.put("suppliers", "suppliers-content.fxml");
        moduleFxml.put("expenses", "expenses-content.fxml");
        moduleFxml.put("employees", "employees-content.fxml");
        moduleFxml.put("reports", "reports-content.fxml");
        moduleFxml.put("dailyClosing", "daily-closing-content.fxml");
        moduleFxml.put("settings", "settings-content.fxml");

        String role = Session.getCurrentUser() != null ? Session.getCurrentUser().getRole() : null;

        if (Session.getCurrentUser() != null) {
            loggedInUserLabel.setText(Session.getCurrentUser().getFullName() + " (" + role + ")");
        }

        for (Map.Entry<String, Button> entry : moduleButtons.entrySet()) {
            boolean allowed = RolePermissions.canAccess(role, entry.getKey());
            Button btn = entry.getValue();
            btn.setVisible(allowed);
            btn.setManaged(allowed);
        }

        String defaultModule = RolePermissions.defaultModule(role);
        if (!RolePermissions.canAccess(role, defaultModule)) {
            defaultModule = moduleButtons.keySet().stream()
                .filter(key -> RolePermissions.canAccess(role, key))
                .findFirst()
                .orElse(null);
        }
        if (defaultModule != null) {
            openModule(defaultModule);
        }
    }

    @FXML
    private void onNavDashboard() {
        openModule("dashboard");
    }

    @FXML
    private void onNavBilling() {
        openModule("billing");
    }

    @FXML
    private void onNavProducts() {
        openModule("products");
    }

    @FXML
    private void onNavInventory() {
        openModule("inventory");
    }

    @FXML
    private void onNavDailyClosing() {
        openModule("dailyClosing");
    }

    @FXML
    private void onNavTableManagement() {
        openModule("tableManagement");
    }

    @FXML
    private void onNavRoomManagement() {
        openModule("roomManagement");
    }

    @FXML
    private void onNavKot() {
        openModule("kot");
    }

    @FXML
    private void onNavPurchase() {
        openModule("purchase");
    }

    @FXML
    private void onNavCustomers() {
        openModule("customers");
    }

    @FXML
    private void onNavSuppliers() {
        openModule("suppliers");
    }

    @FXML
    private void onNavExpenses() {
        openModule("expenses");
    }

    @FXML
    private void onNavEmployees() {
        openModule("employees");
    }

    @FXML
    private void onNavReports() {
        openModule("reports");
    }

    @FXML
    private void onNavSettings() {
        openModule("settings");
    }

    @FXML
    private void onLogout() {
        Session.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/valliento/login.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root, 1366, 800));
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openModule(String moduleKey) {
        String role = Session.getCurrentUser() != null ? Session.getCurrentUser().getRole() : null;
        if (!RolePermissions.canAccess(role, moduleKey)) {
            showError("You do not have permission to access this section.");
            return;
        }
        String fxmlFile = moduleFxml.get(moduleKey);
        Button button = moduleButtons.get(moduleKey);
        if (fxmlFile == null || button == null) return;

        loadScreen(fxmlFile);
        setActive(button);
    }

    private void loadScreen(String fxmlFile) {
        setNavigationEnabled(false);
        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);

        Platform.runLater(() -> {
            try {
                java.net.URL url = getClass().getResource("/com/valliento/" + fxmlFile);
                if (url == null) {
                    showError("Resource not found: /com/valliento/" + fxmlFile);
                    return;
                }
                Parent screen = FXMLLoader.load(url);

                if (contentStack.getChildren().isEmpty()) {
                    contentStack.getChildren().add(screen);
                } else {
                    contentStack.getChildren().set(0, screen);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to load " + fxmlFile + ":\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            } finally {
                loadingOverlay.setVisible(false);
                loadingOverlay.setManaged(false);
                setNavigationEnabled(true);
            }
        });
    }

    private void setNavigationEnabled(boolean enabled) {
        for (Button btn : moduleButtons.values()) {
            btn.setDisable(!enabled);
        }
    }

    private void setActive(Button activeButton) {
        for (Button btn : moduleButtons.values()) {
            btn.getStyleClass().setAll("nav-btn");
        }
        activeButton.getStyleClass().setAll("nav-btn-active");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Screen Load Error");
        alert.showAndWait();
    }
}
