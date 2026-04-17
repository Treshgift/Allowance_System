package com.example.university_allowance_system;

public class TransactionRow {
    private int id;
    private String studentCode;
    private String studentName;
    private String type;
    private double amount;
    private String status;
    private String date;

    public TransactionRow(int id, String studentCode, String studentName, String type, double amount, String status, String date) {
        this.id = id;
        this.studentCode = studentCode;
        this.studentName = studentName;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.date = date;
    }

    public int getId() { return id; }
    public String getStudentCode() { return studentCode; }
    public String getStudentName() { return studentName; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
}

