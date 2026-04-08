package User;

public abstract class User {
    protected int id;
    protected String name;
    protected String username;
    protected String password;
    protected String fullName;
    protected String role;

    public User(int id, String name, String username, String password, String fullName, String role) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }
}
