package com.valliento;

import com.valliento.db.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        DatabaseManager.initializeSchema();

        Parent root = FXMLLoader.load(getClass().getResource("/com/valliento/login.fxml"));
        Scene scene = new Scene(root, 1366, 800);
        stage.setTitle("Valliento POS");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(650);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}