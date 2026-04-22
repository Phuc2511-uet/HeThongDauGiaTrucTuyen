package User;

public class Admin extends User {

    public Admin(String id, String name, String username, String password, String fullName) {
        super(id, name, username, password, fullName);
    }

    @Override
    public void displayInfo() {
        System.out.println("[Admin Account] Admin Name: " + getFullName() + " | Username: " + getUsername());
    }
}