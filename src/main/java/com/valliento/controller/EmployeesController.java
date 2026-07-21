package com.valliento.controller;

import com.valliento.db.EmployeeDAO;
import com.valliento.model.Employee;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class EmployeesController {

    @FXML private TableView<Employee> employeesTable;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private TableColumn<Employee, String> roleColumn;
    @FXML private TableColumn<Employee, String> phoneColumn;
    @FXML private TableColumn<Employee, String> statusColumn;

    @FXML private TextField nameField;
    @FXML private TextField roleField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> statusCombo;

    private final ObservableList<Employee> employeeList = FXCollections.observableArrayList();
    private Employee selectedEmployee = null;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        statusCombo.setItems(FXCollections.observableArrayList("Active", "Inactive"));
        statusCombo.setValue("Active");

        employeesTable.setItems(employeeList);

        employeesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedEmployee = newSel;
            if (newSel != null) {
                nameField.setText(newSel.getName());
                roleField.setText(newSel.getRole());
                phoneField.setText(newSel.getPhone());
                statusCombo.setValue(newSel.getStatus());
            }
        });

        loadEmployees();
    }

    private void loadEmployees() {
        employeeList.setAll(EmployeeDAO.getAllEmployees());
    }

    @FXML
    private void onAddEmployee() {
        String name = nameField.getText().trim();
        String role = roleField.getText().trim();
        if (name.isEmpty() || role.isEmpty()) {
            showAlert("Name and Role are required.");
            return;
        }
        EmployeeDAO.addEmployee(name, role, phoneField.getText().trim());
        clearForm();
        loadEmployees();
    }

    @FXML
    private void onUpdateEmployee() {
        if (selectedEmployee == null) {
            showAlert("Select an employee from the table first.");
            return;
        }
        String name = nameField.getText().trim();
        String role = roleField.getText().trim();
        if (name.isEmpty() || role.isEmpty()) {
            showAlert("Name and Role are required.");
            return;
        }
        EmployeeDAO.updateEmployee(selectedEmployee.getId(), name, role, phoneField.getText().trim(), statusCombo.getValue());
        clearForm();
        loadEmployees();
    }

    @FXML
    private void onDeleteEmployee() {
        if (selectedEmployee == null) {
            showAlert("Select an employee from the table first.");
            return;
        }
        EmployeeDAO.deleteEmployee(selectedEmployee.getId());
        clearForm();
        loadEmployees();
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private void clearForm() {
        nameField.clear();
        roleField.clear();
        phoneField.clear();
        statusCombo.setValue("Active");
        selectedEmployee = null;
        employeesTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}