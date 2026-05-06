package User;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {

    private double ballance = 0;

    public Seller(String id, String username, String password, String fullName) {
        super(id, username, password, fullName);

    }

    @Override
    public void displayInfo() {
        System.out.println("[Seller Account] Name: " + getFullName() + " | Ballance: " + ballance + "⭐");
    }

    public void setBallance(double ballance) {
        this.ballance = ballance;
    }

    public double getBallance() {
        return ballance;
    }
}