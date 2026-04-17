package com.example.university_allowance_system;

public class AccessLogRow {
    private int id;
    private String username;
    private String role;
    private String studentName;
    private String action;
    private boolean allowed;
    private String timestamp;
    private String bankName; // new: which bank (if any) the user belongs to

    public AccessLogRow(int id, String username, String role, String studentName, String action, boolean allowed, String timestamp, String bankName) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.studentName = studentName;
        this.action = action;
        this.allowed = allowed;
        this.timestamp = timestamp;
        this.bankName = bankName;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getStudentName() { return studentName; }
    public String getAction() { return action; }
    public boolean isAllowed() { return allowed; }
    public String getAllowedStatus() { return allowed ? "✅ Allowed" : "❌ Denied"; }
    public String getTimestamp() { return timestamp; }
    public String getBankName() { return bankName; }
}
