package User;

public class Bidder extends User{
    private double balance;
    public Bidder(String id, String name, String username, String password, String fullName,double balance) {
        super(id, name, username, password, fullName);
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
