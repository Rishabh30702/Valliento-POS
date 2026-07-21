package com.valliento.controller;

import com.valliento.db.UserDAO;
import com.valliento.model.User;
import com.valliento.session.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void onLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Enter both username and password.");
            errorLabel.setVisible(true);
            return;
        }

        User user = UserDAO.authenticate(username, password);
        if (user == null) {
            errorLabel.setText("Invalid username or password.");
            errorLabel.setVisible(true);
            return;
        }

        Session.login(user);

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/valliento/main.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 1366, 800);
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Failed to load main screen: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
}