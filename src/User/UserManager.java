package User;

import exceptions.AuthenticationException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import Base.DatabaseManager; // Import DatabaseManager

public class UserManager implements Serializable {

    private static UserManager instance;
    private List<User> users;
    private int count = 0;

    private UserManager() {
        users = new ArrayList<>();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        // Cập nhật count để tránh trùng ID khi tải từ DB
        if (!users.isEmpty()) {
            this.count = users.stream().mapToInt(User::getId).max().orElse(0) + 1;
        }
    }

    // ===== THÊM USER =====
    public boolean addUser(User user) {
        // Duyệt danh sách để kiểm tra username đã tồn tại chưa
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(user.getUsername())) {
                System.out.println("Lỗi: Username " + user.getUsername() + " đã tồn tại!");
                return false; // Trả về false nếu trùng
            }
        }
        users.add(user);
        DatabaseManager.saveUser(user); // Tự động lưu vào DB
        return true; // Chỉ trả về true khi thêm mới thành công
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
        // TODO: Cần thêm logic xóa khỏi DB
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
    public User createUser(String username, String password, String role, String fullName) {

        int id = count++;
        User user;

        switch (role.toUpperCase()) {
            case "BIDDER":
                user = new Bidder(id, username, password, fullName);
                break;
            case "SELLER":
                user = new Seller(id, username, password, fullName);
                break;
            case "ADMIN":
                user = new Admin(id, username, password, fullName);
                break;
            default:
                throw new IllegalArgumentException("Role không hợp lệ");
        }

        users.add(user);
        DatabaseManager.saveUser(user); // Tự động lưu vào DB
        return user;
    }
}