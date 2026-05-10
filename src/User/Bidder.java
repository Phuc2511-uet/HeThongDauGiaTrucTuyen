package User;

import exceptions.InsufficientBalanceException;

public class Bidder extends User{
    private double balance;
    // Constructor dùng khi nạp dữ liệu từ Database
    public Bidder(String username, String password, String fullName, double balance) {
        super(username, password, fullName);
        this.balance = balance;
    }

    // Constructor dùng khi Đăng ký mới (mặc định balance = 0)
    public Bidder(String username, String password, String fullName) {
        super(username, password, fullName);
        this.balance = 0.0;
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
    }

    public void checkBalance(double amount) throws InsufficientBalanceException {
        if (this.balance < amount) {
            throw new InsufficientBalanceException("Tài khoản không đủ số dư để thực hiện đặt giá này!");
        }
    }
}
