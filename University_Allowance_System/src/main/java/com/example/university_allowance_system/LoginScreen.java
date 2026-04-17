package com.example.university_allowance_system;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginScreen {

    public AnchorPane rootPane;
    @FXML private Button loginButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label messageLabel;

    private final UserDao userDao = new UserDao();

    @FXML
    public void initialize() {
        roleCombo.getItems().setAll("Bank", "Student", "Admin");
    }

    @FXML
    public void handleLogin(ActionEvent event) {

        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role = roleCombo.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            messageLabel.setText("Please fill all fields.");
            return;
        }

        //  Database authentication
        boolean ok = userDao.login(username, password, role);

        if (!ok) {
            messageLabel.setText("Invalid credentials.");
            return;
        }

        // Ensure student session is populated: some DBs may have users without student_id set
        if ("Student".equalsIgnoreCase(role) && UserSession.getStudentId() == null) {
            try (java.sql.Connection conn = DatabaseConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM students WHERE student_code = ? LIMIT 1")) {
                ps.setString(1, username);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int sid = rs.getInt("id");
                        // update session with resolved student id
                        UserSession.set(UserSession.getUserId(), UserSession.getUsername(), UserSession.getRole(), UserSession.getBankId(), sid);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        //open the correct dashboard yeah
        try {
            // Debug: print session values to console so we can verify studentId is set
            System.out.println("[DEBUG] Session after login: userId=" + UserSession.getUserId()
                    + " username=" + UserSession.getUsername()
                    + " role=" + UserSession.getRole()
                    + " bankId=" + UserSession.getBankId()
                    + " studentId=" + UserSession.getStudentId());

            switch (role) {
                case "Bank" -> openDashboard("bank-dashboard.fxml", "Bank Dashboard");
                case "Student" -> openDashboard("student-dashboard.fxml", "Student Dashboard");
                case "Admin" -> openDashboard("admin-dashboard.fxml", "Admin Dashboard");
                default -> messageLabel.setText("Unknown role.");
            }
        } catch (Exception e) {
            messageLabel.setText("Could not open dashboard.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleForgotPassword(ActionEvent actionEvent) {
        messageLabel.setText("Password recovery not implemented (demo only).");
    }

    @FXML
    public void handleRegister(ActionEvent actionEvent) {
        messageLabel.setText("Registration screen not implemented yet.");
    }

    private void openDashboard(String fxmlFile, String title) throws IOException {
        Stage stage = (Stage) usernameField.getScene().getWindow();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/university_allowance_system/" + fxmlFile)
        );

        Scene scene = new Scene(loader.load(), 1100, 520);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}