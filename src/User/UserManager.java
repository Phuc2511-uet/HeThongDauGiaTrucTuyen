package User;

import exceptions.AuthenticationException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserManager implements Serializable {

    private static UserManager instance;
    private List<User> users;

    private UserManager() {
        users = new ArrayList<>();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // ===== THÊM USER =====
    public boolean addUser(User user) {
        users.add(user);
        return true;
    }

    // ===== LOGIN =====
    public User authenticate(String username, String password) throws AuthenticationException {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                u.login(password);
                return u;
            }
        }
        throw new AuthenticationException("Tài khoản không tồn tại!");
    }

    // ===== LẤY THEO ID (int) =====
    public User getById(int id) {
        for (User u : users) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }
    public boolean removeUser(int id) {
        return users.removeIf(u -> u.getId() == id);
    }
    public String getAllUserIdsAsString() {
        return "USER_IDS " +
                users.stream()
                        .map(u -> String.valueOf(u.getId()))
                        .reduce((a, b) -> a + " " + b)
                        .orElse("");
    }
    public String getUserInfoAsString(int id) {

        User u = getById(id);

        if (u == null) {
            return "ERROR USER NOT FOUND";
        }

        String role = "UNKNOWN";

        if (u instanceof Bidder) role = "BIDDER";
        else if (u instanceof Seller) role = "SELLER";
        else if (u instanceof Admin) role = "ADMIN";

        return "USER_DETAIL "
                + u.getId() + " "
                + u.getUsername() + " "
                + role + " "
                + u.getFullName().replace(" ", "_");
    }

    public List<User> getUsers() {
        return users;
    }

    public static void setInstance(UserManager loadedInstance) {
        instance = loadedInstance;
    }
}