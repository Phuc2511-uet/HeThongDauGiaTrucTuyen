package User;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {

    private double ballance = 0;

    public Seller(int id,String username, String password, String fullName) {
        super( id,username, password, fullName);

    }

    @Override
    public void displayInfo() {
        System.out.println("[Seller Account] Name: " + getFullName() + " | Ballance: " + ballance + "⭐");
    }

    public void setBalance(double ballance) {
        this.ballance = ballance;
    }

    public double getBalance() {
        return ballance;
    }
}