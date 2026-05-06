package User;

import exceptions.InsufficientBalanceException;

public class Bidder extends User{
    private double balance;
    public Bidder(String username, String password, String fullName) {
        super( username, password, fullName);
        this.balance = 0;
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

    @Override
    public void update(String message) {
        Platform.runLater(() -> {
            // Cập nhật giao diện ở đây
            txtNotification.appendText(message + "\n");
            lblCurrentPrice.setText(extractPrice(message));
        });
    }
}
