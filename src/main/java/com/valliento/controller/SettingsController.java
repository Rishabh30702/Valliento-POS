package com.valliento.controller;

import com.valliento.db.SettingsDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class SettingsController {

    @FXML private TextField businessNameField;
    @FXML private TextField addressField;
    @FXML private TextField gstinField;
    @FXML private TextField phoneField;
    @FXML private TextField currencyField;

    @FXML
    public void initialize() {
        businessNameField.setText(SettingsDAO.getSetting("business_name", ""));
        addressField.setText(SettingsDAO.getSetting("address", ""));
        gstinField.setText(SettingsDAO.getSetting("gstin", ""));
        phoneField.setText(SettingsDAO.getSetting("phone", ""));
        currencyField.setText(SettingsDAO.getSetting("currency", "INR - ₹"));
    }

    @FXML
    private void onSaveSettings() {
        SettingsDAO.saveSetting("business_name", businessNameField.getText().trim());
        SettingsDAO.saveSetting("address", addressField.getText().trim());
        SettingsDAO.saveSetting("gstin", gstinField.getText().trim());
        SettingsDAO.saveSetting("phone", phoneField.getText().trim());
        SettingsDAO.saveSetting("currency", currencyField.getText().trim());

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Settings saved successfully.");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}