package User;

public class Admin extends User {

    public Admin( String username, String password, String fullName) {
        super( username, password, fullName);
    }

    @Override
    public void displayInfo() {
        System.out.println("[Admin Account] Admin Name: " + getFullName() + " | Username: " + getUsername());
    }
}