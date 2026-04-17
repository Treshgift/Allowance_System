package com.example.university_allowance_system;

public class AdminStudentRow {

    private final int id;
    private final String studentCode;
    private final String fullName;
    private final String assignedBank;
    private final String status;

    public AdminStudentRow(int id, String studentCode, String fullName, String assignedBank, String status) {
        this.id = id;
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.assignedBank = assignedBank;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAssignedBank() {
        return assignedBank;
    }

    public String getStatus() {
        return status;
    }
}