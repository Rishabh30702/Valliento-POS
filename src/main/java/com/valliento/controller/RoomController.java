package com.valliento.controller;

import com.valliento.db.RoomDAO;
import com.valliento.db.RoomDAO.Room;
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

public class RoomController {

    @FXML private FlowPane roomGrid;
    @FXML private Button addRoomButton;

    @FXML
    public void initialize() {
        if (addRoomButton != null) {
            boolean canManage = canManageRooms();
            addRoomButton.setVisible(canManage);
            addRoomButton.setManaged(canManage);
        }
        loadRooms();
    }

    private boolean canManageRooms() {
        String role = Session.getCurrentUser() != null ? Session.getCurrentUser().getRole() : null;
        return RolePermissions.canManageRooms(role);
    }

    private int currentLocationId() {
        return Session.getCurrentUser() != null
            ? Session.getCurrentUser().getLocationId()
            : com.valliento.db.DatabaseManager.DEFAULT_LOCATION_ID;
    }

    private void loadRooms() {
        roomGrid.getChildren().clear();
        List<Room> rooms = RoomDAO.getAllRooms(currentLocationId());
        for (Room r : rooms) {
            StackPane wrapper = new StackPane();
            wrapper.setPrefSize(90, 70);

            VBox tile = new VBox(4);
            tile.getStyleClass().add("table-tile");
            tile.setPrefSize(90, 70);
            tile.setStyle(tileColor(r.status));
            tile.setAlignment(Pos.CENTER);

            Label nameLabel = new Label(r.roomNo);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");

            Label statusLabel = new Label(r.status);
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white;");

            tile.getChildren().addAll(nameLabel, statusLabel);

            if (RoomDAO.STATUS_BOOKED.equals(r.status) && r.price != null) {
                Label priceLabel = new Label(String.format("\u20b9%.2f", r.price));
                priceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white;");
                tile.getChildren().add(priceLabel);
            }

            final int roomId = r.id;
            final String currentStatus = r.status;
            final String roomNo = r.roomNo;
            tile.setOnMouseClicked(e -> cycleStatus(roomId, currentStatus));

            if (canManageRooms()) {
                // Small delete button pinned to the top-right corner of the tile.
                Button deleteBtn = new Button("\u00d7");
                deleteBtn.setStyle(
                    "-fx-background-color: rgba(0,0,0,0.35); -fx-text-fill: white; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 18;" +
                    "-fx-min-height: 18; -fx-max-width: 18; -fx-max-height: 18; " +
                    "-fx-background-radius: 9; -fx-padding: 0; -fx-cursor: hand;"
                );
                StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);
                deleteBtn.setOnAction(e -> confirmAndDelete(roomId, roomNo));
                wrapper.getChildren().add(deleteBtn);
            }
            wrapper.getChildren().add(tile);
            roomGrid.getChildren().add(wrapper);
        }
    }

    @FXML
    private void onAddRoom() {
        if (!canManageRooms()) {
            showError("You do not have permission to add rooms.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Room");
        dialog.setHeaderText("Enter a name/number for the new room");
        dialog.setContentText("Room No:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String roomNo = result.get().trim();
        if (roomNo.isEmpty()) {
            showError("Room number cannot be empty.");
            return;
        }

        boolean added = RoomDAO.addRoom(roomNo, currentLocationId());
        if (!added) {
            showError("Could not add room \"" + roomNo + "\". It may already exist.");
        }
        loadRooms();
    }

    private void confirmAndDelete(int roomId, String roomNo) {
        if (!canManageRooms()) {
            showError("You do not have permission to delete rooms.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete room \"" + roomNo + "\"? This cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            boolean deleted = RoomDAO.deleteRoom(roomId, currentLocationId());
            if (!deleted) {
                showError("Could not delete this room.");
            }
            loadRooms();
        }
    }

    private void cycleStatus(int roomId, String currentStatus) {
        String next = RoomDAO.nextStatus(currentStatus);

        if (RoomDAO.STATUS_BOOKED.equals(next)) {
            promptForPriceAndBook(roomId);
            return;
        }

        boolean updated = RoomDAO.updateRoomStatus(roomId, next);
        if (!updated) {
            showError("Could not update room status. It may have already changed - refreshing.");
        }
        loadRooms();
    }

    private void promptForPriceAndBook(int roomId) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Book Room");
        dialog.setHeaderText("Enter the price to charge for this room");
        dialog.setContentText("Price (\u20b9):");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        double price;
        try {
            price = Double.parseDouble(result.get().trim());
        } catch (NumberFormatException e) {
            showError("Please enter a valid number for the price.");
            return;
        }

        if (price <= 0) {
            showError("Price must be greater than 0.");
            return;
        }

        boolean booked = RoomDAO.bookRoom(roomId, price);
        if (!booked) {
            showError("Could not book this room - it may have just been booked by someone else. Refreshing.");
        }
        loadRooms();
    }

    private String tileColor(String status) {
        String base = "-fx-background-radius: 8; -fx-cursor: hand; ";
        if (RoomDAO.STATUS_READY.equals(status)) return base + "-fx-background-color: #2e7d32;";
        if (RoomDAO.STATUS_BOOKED.equals(status)) return base + "-fx-background-color: #f5a623;";
        if (RoomDAO.STATUS_CLEANING.equals(status)) return base + "-fx-background-color: #6b7280;";
        return base + "-fx-background-color: #6b7280;";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}