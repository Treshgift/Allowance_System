package com.example.university_allowance_system;

public final class UserSession {

    private static int userId;
    private static String username;
    private static String role;

    // For role=Bank
    private static Integer bankId;

    // For role=Student
    private static Integer studentId;

    private UserSession() {}

    public static void set(int id, String uname, String r, Integer bId, Integer sId) {
        userId = id;
        username = uname;
        role = r;
        bankId = bId;
        studentId = sId;
    }

    public static void clear() {
        userId = 0;
        username = null;
        role = null;
        bankId = null;
        studentId = null;
    }

    public static int getUserId() { return userId; }
    public static String getUsername() { return username; }
    public static String getRole() { return role; }
    public static Integer getBankId() { return bankId; }
    public static Integer getStudentId() { return studentId; }
}