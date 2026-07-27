package com.valliento.util;

import com.valliento.AppVersion;
import com.valliento.db.SettingsDAO;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class UpdateChecker {

    // How long "Remind Me Later" should actually suppress the popup for.
    private static final int SNOOZE_DAYS = 1;

    public static void checkForUpdateIfAdmin(String userRole) {
        if (!"Administrator".equals(userRole)) {
            return;
        }

        String latestVersion = SettingsDAO.get("latest_app_version", AppVersion.CURRENT_VERSION);
        String downloadUrl = SettingsDAO.get("update_download_url", "");

        if (!isNewer(latestVersion, AppVersion.CURRENT_VERSION)) {
            return;
        }

        // Don't re-show if the user already saw + snoozed this exact version recently.
        if (isSnoozed(latestVersion)) {
            return;
        }

        showUpdateReminder(latestVersion, downloadUrl);
    }

    private static boolean isSnoozed(String latestVersion) {
        String snoozedVersion = SettingsDAO.get("update_snoozed_version", "");
        String snoozedUntilStr = SettingsDAO.get("update_snoozed_until", "");

        // Only honour the snooze if it was set for this exact version - a newer
        // update should still interrupt an old snooze.
        if (!latestVersion.equals(snoozedVersion) || snoozedUntilStr.isBlank()) {
            return false;
        }

        try {
            LocalDate snoozedUntil = LocalDate.parse(snoozedUntilStr);
            return !LocalDate.now().isAfter(snoozedUntil);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static void snoozeUntilTomorrow(String latestVersion) {
        SettingsDAO.set("update_snoozed_version", latestVersion);
        SettingsDAO.set("update_snoozed_until", LocalDate.now().plusDays(SNOOZE_DAYS).toString());
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
            "Choose \"Remind Me Later\" and this won't ask again until tomorrow."
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
        } else {
            // "Remind Me Later" or dialog dismissed some other way (e.g. closed with X).
            snoozeUntilTomorrow(latestVersion);
        }
    }
}