package User;

public class Bidder extends User{
    private int balance;
    public Bidder(String id, String name, String username, String password, String fullName, String role,int balance) {
        super(id, name, username, password, fullName, role);
        this.balance = balance;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }
}
