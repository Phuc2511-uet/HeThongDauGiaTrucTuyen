package User;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    private double rating; // Đánh giá của người bán (ví dụ: 4.5/5.0)

<<<<<<< Updated upstream
    public Seller(String id, String name, String username, String password, String fullName, String role, double rating) {
        super(id, name, username, password, fullName, role);
=======
    public Seller(String id, String name, String username, String password, String fullName, double rating) {
        super(id, name, username, password, fullName);
>>>>>>> Stashed changes
        this.rating = rating;
    }

    @Override
    public void displayInfo() {
        System.out.println("[Seller Account] Name: " + getFullName() + " | Rating: " + rating + "⭐");
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}