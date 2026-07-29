package com.valliento.controller;

import com.valliento.db.UserDAO;
import com.valliento.model.User;
import com.valliento.session.Session;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loadingIndicator;

    /** Holds everything computed on the background thread, so the FX thread only reacts to a finished result. */
    private static class LoginResult {
        User user;
        boolean subscriptionValid = true;
    }

    @FXML
    private void onLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Enter both username and password.");
            errorLabel.setVisible(true);
            return;
        }

        setLoading(true);

        Task<LoginResult> loginTask = new Task<>() {
            @Override
            protected LoginResult call() {
                LoginResult result = new LoginResult();
                result.user = UserDAO.authenticate(username, password);
                if (result.user != null) {
                    result.subscriptionValid = com.valliento.db.SubscriptionDAO.isSubscriptionValid(result.user.getLocationId());
                }
                return result;
            }
        };

        loginTask.setOnSucceeded(e -> {
            LoginResult result = loginTask.getValue();

            if (result.user == null) {
                setLoading(false);
                errorLabel.setText("Invalid username or password.");
                errorLabel.setVisible(true);
                return;
            }

            Session.login(result.user);

            // Subscription must be valid before letting them into the app.
            // If expired, log them straight back out and refuse to load the main screen.
            if (!result.subscriptionValid) {
                Session.logout();
                setLoading(false);
                errorLabel.setText("Your subscription has expired. Please contact support to renew access.");
                errorLabel.setVisible(true);
                return;
            }

            com.valliento.util.UpdateChecker.checkForUpdateIfAdmin(result.user.getRole());

            try {
                Parent root = FXMLLoader.load(getClass().getResource("/com/valliento/main.fxml"));
                Stage stage = (Stage) usernameField.getScene().getWindow();
                Scene scene = new Scene(root, 1366, 800);
                stage.setScene(scene);
                stage.setMaximized(true);
                // no need to setLoading(false) here - this screen is being replaced entirely
            } catch (Exception ex) {
                ex.printStackTrace();
                setLoading(false);
                errorLabel.setText("Failed to load main screen: " + ex.getMessage());
                errorLabel.setVisible(true);
            }
        });

        loginTask.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = loginTask.getException();
            errorLabel.setText("Login failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
            errorLabel.setVisible(true);
        });

        Thread thread = new Thread(loginTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
        if (loading) {
            errorLabel.setVisible(false);
        }
    }
}