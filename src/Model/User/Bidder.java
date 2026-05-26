package Model.User;

import Model.Observer.Observer;
import Controllers.Exceptions.InsufficientBalanceException;
import Controllers.Base.DatabaseManager;

import java.io.PrintWriter;

public class Bidder extends User implements Observer {
    private double balance;
    private double reservedBalance = 0;
    private transient PrintWriter out;

    // Constructor 1: Cho người dùng mới đăng ký
    public Bidder(int id, String username, String password, String fullName) {
        super(id, username, password, fullName);
        this.balance = 0;
        this.reservedBalance = 0;
    }

    // Constructor 2: Dùng để nạp dữ liệu từ Database lên RAM
    public Bidder(int id, String username, String password, String fullName, double balance, double reservedBalance) {
        super(id, username, password, fullName);
        this.balance = balance;
        this.reservedBalance = reservedBalance;
    }

    // Nghiệp vụ tính số dư khả dụng
    public double getAvailableBalance() {
        return balance - reservedBalance;
    }

    // Đóng băng tiền + Tự động cập nhật xuống DB
    public synchronized void reserve(double amount) {
        reservedBalance += amount;
        DatabaseManager.updateUserState(this);
     }
 
     // Giải phóng tiền + Tự động cập nhật xuống DB
     public synchronized void release(double amount) {
         reservedBalance -= amount;
         if (reservedBalance < 0) reservedBalance = 0;
         DatabaseManager.updateUserState(this);
     }
 
     public void checkBalance(double amount) throws InsufficientBalanceException {
         if (this.getAvailableBalance() < amount) {
             throw new InsufficientBalanceException("Không_đủ_số_dư_khả_dụng");
         }
     }
 
     public synchronized boolean deposit(double amount) {
         if (amount <= 0) return false;
         this.balance += amount;
         DatabaseManager.updateUserState(this);
         return true;
     }
 
     // Getter và Setter cho DatabaseManager truy cập
     public double getBalance() { return balance; }
     public synchronized void setBalance(double balance) {
         this.balance = balance;
         DatabaseManager.updateUserState(this);
     }
 
     public double getReservedBalance() { return reservedBalance; }
     public synchronized void setReservedBalance(double reservedBalance) {
         this.reservedBalance = reservedBalance;
         DatabaseManager.updateUserState(this);
     }
 
     public synchronized void setConnection(PrintWriter out) { this.out = out; }
 
     public synchronized void clearConnection(PrintWriter oldOut) {
         if (this.out == oldOut) {
             this.out = null;
         }
     }
 
     @Override
     public synchronized void update(String message) {
         if (out != null) out.println(message);
     }

    @Override
    public void displayInfo() {
        System.out.println("[Bidder] Name: " + getFullName() + " | Balance: " + balance + " | Reserved: " + reservedBalance);
    }
}