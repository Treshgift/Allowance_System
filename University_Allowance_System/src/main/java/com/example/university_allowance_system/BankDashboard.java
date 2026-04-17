package com.example.university_allowance_system;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import java.util.List;

public class BankDashboard {

    @FXML private Label bankNameLabel;
    @FXML private Label welcomeLabel;

    @FXML private Label totalStudentsLabel;
    @FXML private Label totalDisbursedLabel;
    @FXML private Label pendingLabel;

    @FXML private TextField searchField;
    @FXML private TableView<StudentRow> studentsTable;
    @FXML private TableColumn<StudentRow, String> colName;
    @FXML private TableColumn<StudentRow, String> colStudentId;
    @FXML private TableColumn<StudentRow, String> colBalance;
    @FXML private TableColumn<StudentRow, String> colMonth;
    @FXML private TableColumn<StudentRow, String> colStatus;

    @FXML private Label tableInfoLabel;

    @FXML private Label detailNameLabel;
    @FXML private Label detailIdLabel;
    @FXML private Label detailBalanceLabel;
    @FXML private Label detailLoanAmountLabel;
    @FXML private Label detailLoanStatusLabel;

    @FXML private Label accessMessageLabel;

    private final BankDao bankDao = new BankDao();
    private final AdminDao adminDao = new AdminDao();
    private Integer bankId;

    @FXML
    public void initialize() {

        bankId = UserSession.getBankId();

        String bankName = bankDao.getBankNameById(bankId);
        bankNameLabel.setText(bankName);
        welcomeLabel.setText("Hello, " + bankName + " Bank Staff");

        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentCode"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colMonth.setCellValueFactory(new PropertyValueFactory<>("month"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        clearDetailsPanel();
        loadStudents("");

        // 🔥 Chinese Wall Enforcement
        studentsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, student) -> {
                    if (student != null) {
                        showStudentDetails(student);
                    }
                }
        );

        searchField.textProperty().addListener(
                (obs, oldValue, newValue) -> loadStudents(newValue)
        );

        // 🔥 Optional visual lock
        studentsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(StudentRow row, boolean empty) {
                super.updateItem(row, empty);

                if (row == null || empty) {
                    setStyle("");
                } else if (row.getBankId() != bankId) {
                    setStyle("-fx-background-color: #3a1f1f; -fx-text-fill: gray;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    /// the months class
    private List<String> generateMonths() {

        List<String> months = new ArrayList<>();

        int currentYear = LocalDate.now().getYear();

        for (Month month : Month.values()) {
            String formatted = month.toString() + " " + currentYear;
            months.add(formatted);
        }

        return months;
    }

    private void loadStudents(String keyword) {

        List<StudentRow> students = StudentDao.getStudentsForBank(bankId, keyword);
        studentsTable.getItems().setAll(students);

        totalStudentsLabel.setText(String.valueOf(StudentDao.countStudentsForBank(bankId)));
        totalDisbursedLabel.setText(StudentDao.getTotalDisbursedForBank(bankId));
        pendingLabel.setText(String.valueOf(StudentDao.getPendingCountForBank(bankId)));

        tableInfoLabel.setText("Showing " + students.size() + " student(s)");
    }

    private void showStudentDetails(StudentRow student) {

        int studentBank = student.getBankId();
        int studentId = student.getId();
        Integer userId = UserSession.getUserId();

        // 🔥 CHINESE WALL BLOCK
        if (studentBank != bankId) {

            clearDetailsPanel();

            accessMessageLabel.setText(
                    "🚫 Access denied: Student belongs to another bank."
            );

            // record denied access attempt
            adminDao.recordAccess(userId, studentId, "VIEW_STUDENT", false);

            return;
        }

        // record allowed access
        adminDao.recordAccess(userId, studentId, "VIEW_STUDENT", true);

        accessMessageLabel.setText("");

        detailNameLabel.setText(student.getFullName());
        detailIdLabel.setText(student.getStudentCode());
        detailBalanceLabel.setText(student.getBalance());
        detailLoanAmountLabel.setText(student.getLoanAmount());
        detailLoanStatusLabel.setText(student.getLoanStatus());
    }

    private void clearDetailsPanel() {
        detailNameLabel.setText("-");
        detailIdLabel.setText("-");
        detailBalanceLabel.setText("K 0");
        detailLoanAmountLabel.setText("K 0");
        detailLoanStatusLabel.setText("-");
    }

    @FXML
    public void handleLogout(ActionEvent event) {

        try {
            UserSession.clear();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/university_allowance_system/Login_Screen.fxml")
            );

            Scene scene = new Scene(loader.load(), 1100, 520);

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();

        } catch (Exception e) {
            System.out.println("Failed to log out");
        }
    }

    public void handleDisburse(ActionEvent actionEvent) {

        if (bankId == null) {
            accessMessageLabel.setText("No bank session found.");
            return;
        }

        try {
            // 💰 Ask amount
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Batch Disbursement");
            dialog.setHeaderText("Enter Allowance Amount ");

            String input = dialog.showAndWait().orElse(null);
            if (input == null) return;

            double amount = Double.parseDouble(input);

            //  NEW: dynamic month selection
            List<String> months = generateMonths();

            ChoiceDialog<String> monthDialog = new ChoiceDialog<>(
                    months.get(LocalDate.now().getMonthValue() - 1),
                    months
            );

            monthDialog.setTitle("Select Disbursement Month");
            monthDialog.setHeaderText("Choose Month for Allowance");

            String month = monthDialog.showAndWait().orElse(null);
            if (month == null) return;

            // 💸 Disburse
            int processed = StudentDao.disburseAllowancesForBank(
                    bankId,
                    amount,
                    month
            );

            accessMessageLabel.setText(
                    "✅ Disbursed to " + processed + " student(s) for " + month
            );

            loadStudents(searchField.getText());

        } catch (Exception e) {
            accessMessageLabel.setText("Invalid amount entered.");
        }
    }

    public void handlePrev(ActionEvent actionEvent) {
    }

    public void handleNext(ActionEvent actionEvent) {
    }

    public void handleProcessLoan(ActionEvent actionEvent) {
    }

    public void handleViewStudent(ActionEvent actionEvent) {
    }

    public void handleProcessAllowance(ActionEvent actionEvent) {

    }
}