package User;

public class Admin extends User {

    public Admin(String id, String name, String username, String password, String fullName, String role) {
<<<<<<< Updated upstream
        super(id, name, username, password, fullName, role);
=======
        super(id, name, username, password, fullName);
>>>>>>> Stashed changes
    }

    @Override
    public void displayInfo() {
        System.out.println("[Admin Account] Admin Name: " + getFullName() + " | Username: " + getUsername());
    }
}