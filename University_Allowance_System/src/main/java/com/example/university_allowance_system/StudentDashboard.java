package com.example.university_allowance_system;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.util.Duration;
import java.util.logging.Logger;
import java.util.logging.Level;

public class StudentDashboard {

    private static final Logger LOGGER = Logger.getLogger(StudentDashboard.class.getName());

    @FXML private TableView<AllowanceRow> historyTable;
    @FXML private TableColumn<AllowanceRow, String> colMonth;
    @FXML private TableColumn<AllowanceRow, String> colAmount;
    @FXML private TableColumn<AllowanceRow, String> colStatus;

    @FXML private Label studentNameLabel;
    @FXML private Label assignedBankLabel;
    @FXML private Label balanceLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label statusLabel;

    @FXML private Label loanAmountLabel;
    @FXML private Label loanStatusLabel;

    @FXML private Label allowanceMonthLabel;
    @FXML private Label allowanceAmountLabel;
    @FXML private Label allowanceStatusLabel;

    @FXML private Label noticeLabel;

    @FXML
    public void initialize() {
        // Prevent initialize being executed more than once (FXML can sometimes create multiple instances)
        if (initialized) return;
        initialized = true;

        // Load asynchronously so UI remains responsive and values are fresh
        loadStudentDetailsAsync();
        loadLoanDetailsAsync();
        loadAllowanceDetailsAsync();
        loadAllowanceHistoryAsync();

        styleTransactionTable();

        // Ensure labels have sensible defaults to avoid empty UI
        if (loanAmountLabel != null) loanAmountLabel.setText("K 0");
        if (loanStatusLabel != null) loanStatusLabel.setText("No Loan");
        if (allowanceMonthLabel != null) allowanceMonthLabel.setText("-");
        if (allowanceAmountLabel != null) allowanceAmountLabel.setText("K 0");
        if (allowanceStatusLabel != null) allowanceStatusLabel.setText("No Record");
        if (noticeLabel != null) noticeLabel.setText("Keep your bank details updated.");

        // Auto-refresh every 10 seconds so student sees bank-entered updates
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(10), ev -> refreshAll()));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
    }

    // Async wrappers
    private void loadStudentDetailsAsync() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                loadStudentDetails();
                return null;
            }
        };
        new Thread(task).start();
    }

    private void loadLoanDetailsAsync() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                loadLoanDetails();
                return null;
            }
        };
        new Thread(task).start();
    }

    private void loadAllowanceDetailsAsync() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                loadAllowanceDetails();
                return null;
            }
        };
        new Thread(task).start();
    }

    private void loadAllowanceHistoryAsync() {
        Task<List<AllowanceRow>> task = new Task<>() {
            @Override
            protected List<AllowanceRow> call() throws Exception {
                List<AllowanceRow> list = new ArrayList<>();

                Integer studentId = getOrResolveStudentId();
                if (studentId == null) return list;

                String sql = "SELECT month, amount, status FROM allowances WHERE student_id=? ORDER BY id DESC";

                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, studentId);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        AllowanceRow row = new AllowanceRow(
                                rs.getString("month"),
                                "K " + (int) rs.getDouble("amount"),
                                rs.getString("status")
                        );
                        list.add(row);
                    }

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load allowance history (async)", e);
                }

                return list;
            }
        };

        task.setOnSucceeded(e -> {
            List<AllowanceRow> deduped = task.getValue();
            // Deduplicate preserving order
            Set<String> seen = new LinkedHashSet<>();
            List<AllowanceRow> unique = new ArrayList<>();
            for (AllowanceRow ar : deduped) {
                String key = ar.getMonth() + "|" + ar.getAmount() + "|" + ar.getStatus();
                if (!seen.contains(key)) {
                    seen.add(key);
                    unique.add(ar);
                }
            }

            Platform.runLater(() -> historyTable.setItems(FXCollections.observableArrayList(unique)));
        });

        new Thread(task).start();
    }

    // make this an instance flag so each controller instance runs initialize once
    private boolean initialized = false;

    // guard against rapid duplicate refreshes
    private long lastHistoryLoadMillis = 0L;

    private void loadStudentDetails() {

        Integer studentId = getOrResolveStudentId();
        if (studentId == null) return;

        String sql = """
            SELECT s.student_code, s.full_name, s.status, b.name, s.balance
            FROM students s
            JOIN banks b ON s.assigned_bank_id = b.id
            WHERE s.id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                final String fullName = rs.getString("full_name");
                final String code = rs.getString("student_code");
                final String bankName = rs.getString("name");
                final String status = rs.getString("status");

                // Safely read balance: some DBs may not yet have the balance column
                double bal = 0;
                try {
                    java.sql.ResultSetMetaData md = rs.getMetaData();
                    boolean hasBalance = false;
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        if ("balance".equalsIgnoreCase(md.getColumnName(i))) {
                            hasBalance = true;
                            break;
                        }
                    }

                    if (hasBalance) {
                        bal = rs.getDouble("balance");
                        if (rs.wasNull()) bal = 0;
                    } else {
                        // fallback: compute balance as sum of paid allowances for this student
                        try (Connection conn2 = DatabaseConnection.getConnection();
                             PreparedStatement psSum = conn2.prepareStatement(
                                     "SELECT COALESCE(SUM(amount),0) FROM allowances WHERE student_id = ? AND status = 'Paid'")) {
                            psSum.setInt(1, studentId);
                            ResultSet rsSum = psSum.executeQuery();
                            if (rsSum.next()) {
                                bal = rsSum.getDouble(1);
                            }
                        }
                    }
                } catch (Exception ex) {
                    bal = 0;
                }

                final double finalBal = bal;

                Platform.runLater(() -> {
                    if (studentNameLabel != null) studentNameLabel.setText("Student: " + fullName);
                    if (studentIdLabel != null) studentIdLabel.setText(code);
                    if (assignedBankLabel != null) assignedBankLabel.setText(bankName);
                    if (balanceLabel != null) balanceLabel.setText("K " + (int) finalBal);
                    if (statusLabel != null) statusLabel.setText(status);
                });
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading student details", e);
        }
    }

    private void loadLoanDetails() {

        Integer studentId = getOrResolveStudentId();
        if (studentId == null) return;

        String sql = "SELECT amount, status FROM loans WHERE student_id=? ORDER BY id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            final String amt;
            final String st;

            if (rs.next()) {
                amt = "K " + (int) rs.getDouble("amount");
                st = rs.getString("status");
            } else {
                amt = "K 0";
                st = "No Loan";
            }

            Platform.runLater(() -> {
                if (loanAmountLabel != null) loanAmountLabel.setText(amt);
                if (loanStatusLabel != null) loanStatusLabel.setText(st);
            });

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading loan details", e);
        }
    }

    private void loadAllowanceDetails() {

        Integer studentId = getOrResolveStudentId();
        if (studentId == null) return;

        String sql = "SELECT month, amount, status FROM allowances WHERE student_id=? ORDER BY id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            final String monthVal;
            final String amountVal;
            final String statusVal;

            if (rs.next()) {
                monthVal = rs.getString("month");
                amountVal = "K " + (int) rs.getDouble("amount");
                statusVal = rs.getString("status");
            } else {
                monthVal = "-";
                amountVal = "K 0";
                statusVal = "No Record";
            }

            Platform.runLater(() -> {
                if (allowanceMonthLabel != null) allowanceMonthLabel.setText(monthVal);
                if (allowanceAmountLabel != null) allowanceAmountLabel.setText(amountVal);
                if (allowanceStatusLabel != null) allowanceStatusLabel.setText(statusVal);
            });

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading allowance details", e);
        }
    }
    // The synchronous loadAllowanceHistory() method was removed; the async loader is used instead.

    private void refreshAll() {
        // use async loaders so UI stays responsive and we avoid updating UI off the FX thread
        loadStudentDetailsAsync();
        loadLoanDetailsAsync();
        loadAllowanceDetailsAsync();
        loadAllowanceHistoryAsync();
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        refreshAll();
    }

    private void styleTransactionTable() {

        colMonth.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getMonth()));

        colAmount.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getAmount()));

        colStatus.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));

        // Status color
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);

                    if (status.equalsIgnoreCase("Paid")) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    } else if (status.equalsIgnoreCase("Pending")) {
                        setStyle("-fx-text-fill: #facc15; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Amount highlight
        colAmount.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String amount, boolean empty) {
                super.updateItem(amount, empty);
                if (!empty && amount != null) {
                    setText(amount);
                    setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                }
            }
        });
    }

    public void handleLogout(ActionEvent event) {

        try {
            UserSession.clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login_Screen.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 520);

            Stage stage = (Stage) studentNameLabel.getScene().getWindow();
            stage.setScene(scene);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load login screen on logout", e);
        }
    }

    /**
     * Resolve a student id to use for queries. Preference order:
     *  - UserSession.studentId if present
     *  - users.student_id (lookup by username)
     *  - students. I'd (lookup by student_code == username)
     * If found, the UserSession is updated so subsequent calls can use it directly.
     */
    private Integer getOrResolveStudentId() {
        try {
            Integer sid = UserSession.getStudentId();
            if (sid != null) return sid;

            String uname = UserSession.getUsername();
            if (uname == null) return null;

            try (Connection conn = DatabaseConnection.getConnection()) {
                // 1) check users.student_id
                String q1 = "SELECT student_id FROM users WHERE username = ? LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(q1)) {
                    ps.setString(1, uname);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int v = rs.getInt("student_id");
                            if (!rs.wasNull()) {
                                UserSession.set(UserSession.getUserId(), uname, UserSession.getRole(), UserSession.getBankId(), v);
                                return v;
                            }
                        }
                    }
                }

                // 2) fallback: students table by student_code
                String q2 = "SELECT id FROM students WHERE student_code = ? LIMIT 1";
                try (PreparedStatement ps2 = conn.prepareStatement(q2)) {
                    ps2.setString(1, uname);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) {
                            int v2 = rs2.getInt("id");
                            UserSession.set(UserSession.getUserId(), uname, UserSession.getRole(), UserSession.getBankId(), v2);
                            return v2;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}