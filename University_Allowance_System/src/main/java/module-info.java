module com.example.university_allowance_system {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires java.sql;

    opens com.example.university_allowance_system to javafx.fxml;
    exports com.example.university_allowance_system;
}