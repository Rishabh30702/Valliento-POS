package com.valliento.backup;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupManager {

    private static final String DB_FILE_NAME = "valliento.db";

    public static String getDbPath() {
        return Paths.get(System.getProperty("user.dir"), DB_FILE_NAME).toString();
    }

    public static String suggestedBackupFileName() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        return "valliento-backup-" + timestamp + ".db";
    }

    public static void createBackup(String destinationPath) throws IOException {
        Path source = Paths.get(getDbPath());
        Path destination = Paths.get(destinationPath);
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void restoreBackup(String backupFilePath) throws IOException {
        Path source = Paths.get(backupFilePath);
        Path destination = Paths.get(getDbPath());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public static long getDbSizeBytes() {
        try {
            return Files.size(Paths.get(getDbPath()));
        } catch (IOException e) {
            return 0;
        }
    }

    public static String getDbSizeFormatted() {
        long bytes = getDbSizeBytes();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}