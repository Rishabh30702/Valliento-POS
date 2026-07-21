package com.valliento.controller;

import com.valliento.db.TableDAO;
import com.valliento.model.RestaurantTable;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class TableController {

    @FXML private FlowPane tableGrid;

    @FXML
    public void initialize() {
        loadTables();
    }

    private void loadTables() {
        tableGrid.getChildren().clear();
        List<RestaurantTable> tables = TableDAO.getAllTables();
        for (RestaurantTable t : tables) {
            VBox tile = new VBox(4);
            tile.getStyleClass().add("table-tile");
            tile.setPrefSize(90, 70);
            tile.setStyle(tileColor(t.getStatus()));

            Label nameLabel = new Label(t.getTableNo());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");

            Label statusLabel = new Label(t.getStatus());
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white;");

            tile.getChildren().addAll(nameLabel, statusLabel);
            tile.setAlignment(javafx.geometry.Pos.CENTER);

            final int tableId = t.getId();
            final String currentStatus = t.getStatus();
            tile.setOnMouseClicked(e -> cycleStatus(tableId, currentStatus));

            tableGrid.getChildren().add(tile);
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
}