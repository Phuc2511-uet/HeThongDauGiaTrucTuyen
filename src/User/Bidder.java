package User;

import exceptions.InsufficientBalanceException;

public class Bidder extends User{
    private double balance;
    public Bidder(String id, String name, String username, String password, String fullName,double balance) {
        super(id, name, username, password, fullName);
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
    }

    public void checkBalance(int amount) throws InsufficientBalanceException {
        if (this.balance < amount) {
            throw new InsufficientBalanceException("Tài khoản không đủ số dư để thực hiện đặt giá này!");
        }
    }
}
