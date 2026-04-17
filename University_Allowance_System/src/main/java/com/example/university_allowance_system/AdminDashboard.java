package com.example.university_allowance_system;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class AdminDashboard {
    // UI fields (annotated with @FXML to ensure FXMLLoader injects them)
    @FXML public TabPane adminTabs;
    @FXML public TextField transactionSearchField;
    @FXML public TableColumn<TransactionRow, Integer> colTxnId;
    @FXML public TableColumn<TransactionRow, String> colTxnStudent;
    @FXML public TableColumn<TransactionRow, String> colTxnType;
    @FXML public TableColumn<TransactionRow, Double> colTxnAmount;
    @FXML public TableColumn<TransactionRow, String> colTxnStatus;
    @FXML public TableColumn<TransactionRow, String> colTxnDate;
    @FXML public TextField accessSearchField;
    @FXML public TableColumn<AccessLogRow, Integer> colLogId;
    @FXML public TableColumn<AccessLogRow, String> colLogUser;
    @FXML public TableColumn<AccessLogRow, String> colLogRole;
    @FXML public TableColumn<AccessLogRow, String> colLogStudent;
    @FXML public TableColumn<AccessLogRow, String> colLogAction;
    @FXML public TableColumn<AccessLogRow, String> colLogAllowed;
    @FXML public TableColumn<AccessLogRow, String> colLogTime;
    @FXML public TableColumn<AccessLogRow, String> colLogBank;
    @FXML public ToggleButton darkModeToggle;
    @FXML public Pagination studentPagination;
    @FXML private TableColumn<AdminStudentRow, Void> colEditStudent;
    @FXML private TableColumn<AdminStudentRow, Void> colDeleteStudent;
    @FXML private Label adminNameLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalBanksLabel;
    @FXML private Label transactionsLabel;
    @FXML private Label deniedAccessLabel;

    @FXML private TextField studentSearchField;
    @FXML private TableView<AdminStudentRow> studentsTable;
    @FXML private TableColumn<AdminStudentRow, String> colStudentId;
    @FXML private TableColumn<AdminStudentRow, String> colStudentName;
    @FXML private TableColumn<AdminStudentRow, String> colAssignedBank;
    @FXML private TableColumn<AdminStudentRow, String> colStudentStatus;

    @FXML private TextField bankSearchField;
    @FXML private TableView<BankRow> banksTable;
    @FXML private TableColumn<BankRow, Integer> colBankId;
    @FXML private TableColumn<BankRow, String> colBankName;
    @FXML private TableColumn<BankRow, Integer> colBankUsers;

    @FXML private ComboBox<String> transactionTypeCombo;
    @FXML private TableView<TransactionRow> transactionsTable;

    @FXML private ComboBox<String> accessFilterCombo;
    @FXML private TableView<AccessLogRow> accessLogTable;

    private final AdminDao adminDao = new AdminDao();

    @FXML
    public void initialize()
    {

        adminNameLabel.setText("Admin: " + UserSession.getUsername());

        // Students table (guarded in case FXML injection failed for any reason)
        if (colStudentId != null) colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentCode"));
        if (colStudentName != null) colStudentName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        if (colAssignedBank != null) colAssignedBank.setCellValueFactory(new PropertyValueFactory<>("assignedBank"));
        if (colStudentStatus != null) colStudentStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Banks table
        if (colBankId != null) colBankId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colBankName != null) colBankName.setCellValueFactory(new PropertyValueFactory<>("bankName"));
        if (colBankUsers != null) colBankUsers.setCellValueFactory(new PropertyValueFactory<>("users"));

        // Transactions table
        if (colTxnId != null) colTxnId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colTxnStudent != null) colTxnStudent.setCellValueFactory(new PropertyValueFactory<>("studentCode"));
        if (colTxnType != null) colTxnType.setCellValueFactory(new PropertyValueFactory<>("type"));
        if (colTxnAmount != null) colTxnAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        if (colTxnStatus != null) colTxnStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colTxnDate != null) colTxnDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Access Log table
        if (colLogId != null) colLogId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colLogUser != null) colLogUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        if (colLogRole != null) colLogRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        if (colLogStudent != null) colLogStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        if (colLogAction != null) colLogAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        if (colLogAllowed != null) colLogAllowed.setCellValueFactory(new PropertyValueFactory<>("allowedStatus"));
        if (colLogTime != null) colLogTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        if (colLogBank != null) colLogBank.setCellValueFactory(new PropertyValueFactory<>("bankName"));

        transactionTypeCombo.getItems().addAll("All", "Loan", "Allowance");
        transactionTypeCombo.setValue("All");

        accessFilterCombo.getItems().addAll("All", "Allowed", "Denied");
        accessFilterCombo.setValue("All");

        refreshSummaryCards();
        loadStudents("");
        loadBanks("");
        loadTransactions("");
        loadAccessLogs("");

        studentSearchField.textProperty().addListener((obs, oldVal, newVal) -> loadStudents(newVal));
        bankSearchField.textProperty().addListener((obs, oldVal, newVal) -> loadBanks(newVal));
        transactionSearchField.textProperty().addListener((obs, oldVal, newVal) -> loadTransactions(newVal));
        accessSearchField.textProperty().addListener((obs, oldVal, newVal) -> loadAccessLogs(newVal));
        addEditButton();
        addDeleteButton();
    }

    private void refreshSummaryCards() {
        totalStudentsLabel.setText(String.valueOf(adminDao.countStudents()));
        totalBanksLabel.setText(String.valueOf(adminDao.countBanks()));
        transactionsLabel.setText(String.valueOf(adminDao.countTransactions()));
        deniedAccessLabel.setText(String.valueOf(adminDao.countDeniedAccessAttempts()));
    }

    private void loadStudents(String keyword) {
        studentsTable.getItems().setAll(adminDao.getAllStudents(keyword == null ? "" : keyword.trim()));
    }

    private void loadBanks(String keyword) {
        banksTable.getItems().setAll(adminDao.getAllBanks(keyword == null ? "" : keyword.trim()));
    }

    private void loadTransactions(String keyword) {
        transactionsTable.getItems().setAll(adminDao.getAllTransactions(keyword == null ? "" : keyword.trim()));
    }

    private void loadAccessLogs(String keyword) {
        accessLogTable.getItems().setAll(adminDao.getAllAccessLogs(keyword == null ? "" : keyword.trim()));
    }

    public void handleLogout(ActionEvent actionEvent) {

        try {

            // clear logged user
            UserSession.clear();

            // load login screen
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/university_allowance_system/Login_Screen.fxml")
            );

            Scene scene = new Scene(loader.load(), 1100, 520);

            // get current stage
            Stage stage = (Stage) adminTabs.getScene().getWindow();

            stage.setTitle("Login");
            stage.setScene(scene);
            stage.show();

            System.out.println("Admin logged out");

        }
        catch (Exception e)
        {
          System.out.print("Failed to load Login Screen");
        }
    }

    public void handleAddStudent(ActionEvent actionEvent) {

        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Add Student");
        nameDialog.setHeaderText("Enter student full name");
        nameDialog.setContentText("Full Name:");

        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isEmpty() || nameResult.get().trim().isEmpty()) {
            return;
        }

        List<String> bankNames = adminDao.getBankNames();
        if (bankNames.isEmpty()) {
            showAlert("No banks found. Add a bank first.");
            return;
        }

        ChoiceDialog<String> bankDialog = new ChoiceDialog<>(bankNames.get(0), bankNames);
        bankDialog.setTitle("Assign Bank");
        bankDialog.setHeaderText("Select bank for student");
        bankDialog.setContentText("Bank:");

        Optional<String> bankResult = bankDialog.showAndWait();
        if (bankResult.isEmpty()) {
            return;
        }

        String studentCode = adminDao.generateNextStudentCode();
        boolean added = adminDao.addStudent(studentCode, nameResult.get().trim(), bankResult.get(), "Active");

        if (added) {
            showInfo("Student added successfully.\nStudent Code: " + studentCode);
            loadStudents(studentSearchField.getText());
            refreshSummaryCards();
        } else {
            showAlert("Failed to add student.");
        }
    }

    public void handleRefreshStudents(ActionEvent actionEvent) {
        loadStudents(studentSearchField.getText());
        refreshSummaryCards();
    }

    public void handleAddBank(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Bank");
        dialog.setHeaderText("Enter bank name");
        dialog.setContentText("Bank Name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            return;
        }

        boolean added = adminDao.addBank(result.get().trim());

        if (added) {
            showInfo("Bank added successfully.");
            loadBanks(bankSearchField.getText());
            refreshSummaryCards();
        } else {
            showAlert("Failed to add bank. It may already exist.");
        }
    }

    public void handleRefreshBanks(ActionEvent actionEvent) {
        loadBanks(bankSearchField.getText());
        refreshSummaryCards();
    }

    public void handleRefreshTransactions(ActionEvent actionEvent) {
        loadTransactions(transactionSearchField.getText());
        refreshSummaryCards();
    }

    public void handleRefreshAccessLog(ActionEvent actionEvent) {
        loadAccessLogs(accessSearchField.getText());
        refreshSummaryCards();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void addEditButton() {

        colEditStudent.setCellFactory(param -> new TableCell<>() {

            private final Button btn = new Button("Edit");

            {
                btn.getStyleClass().add("btn-blue");

                btn.setOnAction(event -> {
                    AdminStudentRow student = getTableView().getItems().get(getIndex());

                    TextInputDialog dialog = new TextInputDialog(student.getFullName());
                    dialog.setTitle("Edit Student");
                    dialog.setHeaderText("Update student name");

                    dialog.showAndWait().ifPresent(newName -> {
                        adminDao.updateStudentName(student.getStudentCode(), newName);
                        loadStudents(studentSearchField.getText());
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }
    ///  the delete method in the admin  panel
    private void addDeleteButton() {

        colDeleteStudent.setCellFactory(param -> new TableCell<>() {

            private final Button btn = new Button("Delete");

            {
                btn.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white;");

                btn.setOnAction(event -> {

                    AdminStudentRow student = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Student");
                    confirm.setHeaderText("Delete " + student.getFullName() + "?");

                    if(confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK){

                        adminDao.deleteStudent(student.getStudentCode());

                        loadStudents(studentSearchField.getText());
                        refreshSummaryCards();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty)
            {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }
}
