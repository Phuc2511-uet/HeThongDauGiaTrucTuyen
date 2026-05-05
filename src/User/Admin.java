package User;

public class Admin extends User {

    public Admin(String id, String username, String password, String fullName) {
        super(id, username, password, fullName);
    }

    @Override
    public void displayInfo() {
        System.out.println("[Admin Account] Admin Name: " + getFullName() + " | Username: " + getUsername());
    }
}