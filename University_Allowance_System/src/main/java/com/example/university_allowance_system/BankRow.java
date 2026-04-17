// this is the backend class to load all  the student from the database

package com.example.university_allowance_system;

public class BankRow {

    private final int id;
    private final String bankName;
    private final int users;

    public BankRow(int id, String bankName, int users) {
        this.id = id;
        this.bankName = bankName;
        this.users = users;
    }

    public int getId() {
        return id;
    }

    public String getBankName() {
        return bankName;
    }

    public int getUsers() {
        return users;
    }
}