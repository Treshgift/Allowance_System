package com.example.university_allowance_system;

public class StudentRow {

    private int id;
    private String studentCode;
    private String fullName;
    private String balance;
    private String month;
    private String status;

    private String loanAmount;
    private String loanStatus;

    // 🔥 NEW
    private int bankId;

    public StudentRow(int id,
                      String studentCode,
                      String fullName,
                      String balance,
                      String month,
                      String status,
                      String loanAmount,
                      String loanStatus,
                      int bankId) {

        this.id = id;
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.balance = balance;
        this.month = month;
        this.status = status;
        this.loanAmount = loanAmount;
        this.loanStatus = loanStatus;
        this.bankId = bankId;
    }

    public int getId() { return id; }

    public String getStudentCode() { return studentCode; }

    public String getFullName() { return fullName; }

    public String getBalance() { return balance; }

    public String getMonth() { return month; }

    public String getStatus() { return status; }

    public String getLoanAmount() { return loanAmount; }

    public String getLoanStatus() { return loanStatus; }

    // 🔥 NEW
    public int getBankId() { return bankId; }
}