package com.valliento.controller;

import com.valliento.backup.BackupManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupController {

    @FXML private Label dbSizeLabel;
    @FXML private Label dbPathLabel;
    @FXML private Label lastBackupLabel;

    private Window window;

    @FXML
    public void initialize() {
        dbSizeLabel.setText(BackupManager.getDbSizeFormatted());
        dbPathLabel.setText(BackupManager.getDbPath());
        lastBackupLabel.setText("Not backed up this session");
    }

    @FXML
    private void onCreateBackup() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Backup Location");
        File dir = chooser.showDialog(getWindow());
        if (dir == null) return;

        String fileName = BackupManager.suggestedBackupFileName();
        File destination = new File(dir, fileName);

        try {
            BackupManager.createBackup(destination.getAbsolutePath());
            lastBackupLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
            showAlert(Alert.AlertType.INFORMATION, "Backup created:\n" + destination.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Backup failed: " + e.getMessage());
        }
    }

    @FXML
    private void onRestoreBackup() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Restoring will REPLACE all current data with the backup file.\nThis cannot be undone. Continue?");
        confirm.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Select Backup File to Restore");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Database Files", "*.db"));
                File file = chooser.showOpenDialog(getWindow());
                if (file == null) return;

                try {
                    BackupManager.restoreBackup(file.getAbsolutePath());
                    showAlert(Alert.AlertType.INFORMATION,
                        "Backup restored successfully.\nPlease restart the application for changes to take effect.");
                    dbSizeLabel.setText(BackupManager.getDbSizeFormatted());
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Restore failed: " + e.getMessage());
                }
            }
        });
    }

    private Window getWindow() {
        if (window == null) {
            window = dbSizeLabel.getScene().getWindow();
        }
        return window;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}