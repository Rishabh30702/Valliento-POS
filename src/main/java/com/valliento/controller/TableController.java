package com.valliento.controller;

import com.valliento.db.TableDAO;
import com.valliento.model.RestaurantTable;
import com.valliento.session.RolePermissions;
import com.valliento.session.Session;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

public class TableController {

    @FXML private FlowPane tableGrid;
    @FXML private Button addTableButton;

    @FXML
    public void initialize() {
        if (addTableButton != null) {
            boolean canManage = canManageTables();
            addTableButton.setVisible(canManage);
            addTableButton.setManaged(canManage);
        }
        loadTables();
    }

    private boolean canManageTables() {
        String role = Session.getCurrentUser() != null ? Session.getCurrentUser().getRole() : null;
        return RolePermissions.canManageTables(role);
    }

    private int currentLocationId() {
        return Session.getCurrentUser() != null
            ? Session.getCurrentUser().getLocationId()
            : com.valliento.db.DatabaseManager.DEFAULT_LOCATION_ID;
    }

    private void loadTables() {
        tableGrid.getChildren().clear();
        List<RestaurantTable> tables = TableDAO.getAllTables(currentLocationId());
        for (RestaurantTable t : tables) {
            StackPane wrapper = new StackPane();
            wrapper.setPrefSize(90, 70);

            VBox tile = new VBox(4);
            tile.getStyleClass().add("table-tile");
            tile.setPrefSize(90, 70);
            tile.setStyle(tileColor(t.getStatus()));
            tile.setAlignment(Pos.CENTER);

            Label nameLabel = new Label(t.getTableNo());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");

            Label statusLabel = new Label(t.getStatus());
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white;");

            tile.getChildren().addAll(nameLabel, statusLabel);

            final int tableId = t.getId();
            final String currentStatus = t.getStatus();
            final String tableNo = t.getTableNo();
            tile.setOnMouseClicked(e -> cycleStatus(tableId, currentStatus));

            if (canManageTables()) {
                // Small delete button pinned to the top-right corner of the tile.
                Button deleteBtn = new Button("\u00d7");
                deleteBtn.setStyle(
                    "-fx-background-color: rgba(0,0,0,0.35); -fx-text-fill: white; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 18; " +
                    "-fx-min-height: 18; -fx-max-width: 18; -fx-max-height: 18; " +
                    "-fx-background-radius: 9; -fx-padding: 0; -fx-cursor: hand;"
                );
                StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);
                deleteBtn.setOnAction(e -> confirmAndDelete(tableId, tableNo));
                wrapper.getChildren().add(deleteBtn);
            }
            wrapper.getChildren().add(tile);
            tableGrid.getChildren().add(wrapper);
        }
    }

    @FXML
    private void onAddTable() {
        if (!canManageTables()) {
            showError("You do not have permission to add tables.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Table");
        dialog.setHeaderText("Enter a name/number for the new table");
        dialog.setContentText("Table No:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String tableNo = result.get().trim();
        if (tableNo.isEmpty()) {
            showError("Table number cannot be empty.");
            return;
        }

        boolean added = TableDAO.addTable(tableNo, currentLocationId());
        if (!added) {
            showError("Could not add table \"" + tableNo + "\". It may already exist.");
        }
        loadTables();
    }

    private void confirmAndDelete(int tableId, String tableNo) {
        if (!canManageTables()) {
            showError("You do not have permission to delete tables.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete table \"" + tableNo + "\"? This cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            boolean deleted = TableDAO.deleteTable(tableId, currentLocationId());
            if (!deleted) {
                showError("Could not delete this table.");
            }
            loadTables();
        }
    }

    private void cycleStatus(int tableId, String currentStatus) {
        String next;
        switch (currentStatus) {
            case "Available": next = "Occupied"; break;
            case "Occupied": next = "Reserved"; break;
            case "Reserved": next = "Cleaning"; break;
            default: next = "Available"; break;
        }
        TableDAO.updateTableStatus(tableId, next);
        loadTables();
    }

    private String tileColor(String status) {
        String base = "-fx-background-radius: 8; -fx-cursor: hand; ";
        switch (status) {
            case "Available": return base + "-fx-background-color: #2e7d32;";
            case "Occupied": return base + "-fx-background-color: #f5a623;";
            case "Reserved": return base + "-fx-background-color: #7b5fd1;";
            case "Cleaning": return base + "-fx-background-color: #6b7280;";
            default: return base + "-fx-background-color: #6b7280;";
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}