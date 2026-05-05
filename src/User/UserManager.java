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

    public void addUser(User user) {
        users.add(user);
    }

    // logic tìm kiếm người dùng để đăng nhập
    public User authenticate(String username, String password) throws AuthenticationException {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                u.login(password);
                return u;
            }
        }
        throw new AuthenticationException("Tài khoản không tồn tại!");
    }
    public User getById(String id){
        for (User u : users){
            if (u.getId().equals(id)){
                return u;
            }
        }
        return null;
    }

    public List<User> getUsers() {
        return users;
    }
}