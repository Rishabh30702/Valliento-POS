package com.valliento.controller;

import com.valliento.db.KotDAO;
import com.valliento.model.KotItem;
import com.valliento.model.KotOrder;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class KotController {

    @FXML private VBox kotContainer;

    @FXML
    public void initialize() {
        loadKots();
    }

    private void loadKots() {
        kotContainer.getChildren().clear();
        List<KotOrder> kots = KotDAO.getAllKots();
        for (KotOrder kot : kots) {
            if ("Served".equals(kot.getStatus())) continue;

            VBox card = new VBox(6);
            card.getStyleClass().add("section-card");

            HBox header = new HBox(10);
            Label kotLabel = new Label(kot.getKotNo());
            kotLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            String tableText = kot.getTableNo() != null ? "Table: " + kot.getTableNo() : "Takeaway";
            Label tableLabel = new Label(tableText);
            tableLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
            Label statusLabel = new Label(kot.getStatus());
            statusLabel.setStyle(statusStyle(kot.getStatus()));
            header.getChildren().addAll(kotLabel, tableLabel, statusLabel);

            VBox itemsBox = new VBox(2);
            List<KotItem> items = KotDAO.getItemsForKot(kot.getId());
            for (KotItem item : items) {
                Label itemLabel = new Label(item.getQty() + " x " + item.getProductName());
                itemLabel.setStyle("-fx-font-size: 12px;");
                itemsBox.getChildren().add(itemLabel);
            }

            HBox actions = new HBox(8);
            String next = nextStatus(kot.getStatus());
            if (next != null) {
                Button advanceBtn = new Button("Mark " + next);
                advanceBtn.getStyleClass().add("btn-save");
                final int kotId = kot.getId();
                advanceBtn.setOnAction(e -> {
                    KotDAO.updateKotStatus(kotId, next);
                    loadKots();
                });
                actions.getChildren().add(advanceBtn);
            }

            card.getChildren().addAll(header, itemsBox, actions);
            kotContainer.getChildren().add(card);
        }

        if (kotContainer.getChildren().isEmpty()) {
            Label empty = new Label("No active KOT orders.");
            empty.setStyle("-fx-text-fill: #6b7280;");
            kotContainer.getChildren().add(empty);
        }
    }

    private String nextStatus(String current) {
        switch (current) {
            case "New": return "Preparing";
            case "Preparing": return "Ready";
            case "Ready": return "Served";
            default: return null;
        }
    }

    private String statusStyle(String status) {
        String base = "-fx-font-size: 11px; -fx-padding: 2 8 2 8; -fx-background-radius: 10; -fx-text-fill: white; ";
        switch (status) {
            case "New": return base + "-fx-background-color: #2d6cdf;";
            case "Preparing": return base + "-fx-background-color: #f5a623;";
            case "Ready": return base + "-fx-background-color: #2e7d32;";
            default: return base + "-fx-background-color: #6b7280;";
        }
    }
}