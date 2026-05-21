package Model.User;

import Model.Observer.Observer;
import Controllers.Exceptions.InsufficientBalanceException;
import Controllers.Base.DatabaseManager; // Import DatabaseManager

import java.io.PrintWriter;

public class Bidder extends User implements Observer {
    private double balance;
    private double reservedBalance = 0;
    public double getAvailableBalance() {
        return balance - reservedBalance;
    }

    public void reserve(double amount) {
        reservedBalance += amount;
    }

    public void release(double amount) {
        reservedBalance -= amount;
        if (reservedBalance < 0) reservedBalance = 0;
    }
    public Bidder(int id,String username, String password, String fullName) {
        super( id,username, password, fullName);
        this.balance = 0;
    }

    // Constructor mới để tải từ database
    public Bidder(int id, String username, String password, String fullName, double balance) {
        super(id, username, password, fullName);
        this.balance = balance;
    }


    @Override
    public void displayInfo() {
        System.out.println("[Bidder] Name: " + getFullName() + " | Balance: " + balance);
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
        DatabaseManager.updateUserState(this); // Tự động cập nhật vào DB
    }
    public void checkBalance(double amount) throws InsufficientBalanceException {
        if (this.getAvailableBalance() < amount) {
            throw new InsufficientBalanceException("Không_đủ_số_dư_khả_dụng");
        }
    }


    public boolean deposit(double amount) {

        if (amount <= 0) {
            return false;
        }

        this.balance += amount;
        DatabaseManager.updateUserState(this); // Tự động cập nhật vào DB
        return true;
    }
    private transient PrintWriter out;

    public void setConnection(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void update(String message) {
        if (out != null) {
            out.println(message); // gửi về client
        }
    }
}
