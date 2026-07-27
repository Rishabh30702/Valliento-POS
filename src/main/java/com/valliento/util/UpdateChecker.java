package com.valliento.util;

import com.valliento.AppVersion;
import com.valliento.db.SettingsDAO;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;

public class UpdateChecker {

    public static void checkForUpdateIfAdmin(String userRole) {
        if (!"Administrator".equals(userRole)) {
            return;
        }

        String latestVersion = SettingsDAO.get("latest_app_version", AppVersion.CURRENT_VERSION);
        String downloadUrl = SettingsDAO.get("update_download_url", "");

        if (isNewer(latestVersion, AppVersion.CURRENT_VERSION)) {
            showUpdateReminder(latestVersion, downloadUrl);
        }
    }

    private static boolean isNewer(String remoteVersion, String localVersion) {
        try {
            String[] remoteParts = remoteVersion.trim().split("\\.");
            String[] localParts = localVersion.trim().split("\\.");
            int length = Math.max(remoteParts.length, localParts.length);
            for (int i = 0; i < length; i++) {
                int r = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
                int l = i < localParts.length ? Integer.parseInt(localParts[i]) : 0;
                if (r != l) return r > l;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void showUpdateReminder(String latestVersion, String downloadUrl) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Update Available");
        alert.setHeaderText("Version " + latestVersion + " is available (you're on " + AppVersion.CURRENT_VERSION + ")");
        alert.setContentText(
            "A new version of Valliento POS is ready to install.\n\n" +
            "For the smoothest experience, install this during a quiet time " +
            "(e.g. between shifts) rather than during store hours.\n\n" +
            "This reminder will keep appearing at login until the update is installed."
        );

        ButtonType downloadButton = new ButtonType("Download Update");
        ButtonType laterButton = new ButtonType("Remind Me Later", ButtonType.CANCEL.getButtonData());
        alert.getButtonTypes().setAll(downloadButton, laterButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == downloadButton && downloadUrl != null && !downloadUrl.isBlank()) {
            try {
                Desktop.getDesktop().browse(new URI(downloadUrl));
            } catch (Exception e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR,
                    "Couldn't open the download link automatically. Please visit:\n" + downloadUrl);
                errorAlert.setHeaderText(null);
                errorAlert.showAndWait();
            }
        }
    }
}