package Model.User;

import Controllers.Base.DatabaseManager; // Import DatabaseManager


public class Seller extends User {

    private double balance = 0; // Đã sửa lỗi chính tả từ ballance thành balance

    public Seller(int id,String username, String password, String fullName) {
        super( id,username, password, fullName);

    }

    @Override
    public void displayInfo() {
        System.out.println("[Seller Account] Name: " + getFullName() + " | Balance: " + balance + "⭐");
    }

    public void setBalance(double balance) {
        this.balance = balance;
        DatabaseManager.updateUserState(this); // Tự động cập nhật vào DB
    }

    public double getBalance() {
        return balance;
    }
}