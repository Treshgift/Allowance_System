package com.example.university_allowance_system;

public class AllowanceRow {

    private final String month;
    private final String amount;
    private final String status;

    public AllowanceRow(String month, String amount, String status) {
        this.month = month;
        this.amount = amount;
        this.status = status;
    }

    // 🔹 Required by TableView (JavaFX uses reflection)
    public String getMonth() {
        return month;
    }

    public String getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    // 🔹 Optional (helps debugging + future extensions)
    @Override
    public String toString() {
        return "AllowanceRow{" +
                "month='" + month + '\'' +
                ", amount='" + amount + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}