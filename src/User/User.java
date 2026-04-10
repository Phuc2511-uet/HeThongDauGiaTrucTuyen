package User;
import Base.Entity;

public abstract class User extends Entity {
    protected String name;
    protected String username;
    protected String password;
    protected String fullName;
    protected String role;

    public User(String id, String name, String username, String password, String fullName, String role) {
        super(id);
        this.name = name;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }
}