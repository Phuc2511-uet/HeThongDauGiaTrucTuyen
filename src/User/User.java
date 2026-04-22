package User;
import Base.Entity;
import java.io.Serializable;
import Observer.Observer;

public abstract class User extends Entity implements Serializable,Observer {
    private String name;
    private String username;
    private String password;
    private String fullName;
    private String role;


    public User(String id, String name, String username, String password, String fullName) {
        super(id);
        this.name = name;
        this.username = username;
        this.password = password;
        this.fullName = fullName;

    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public abstract void displayInfo();

    @Override
    public void update(String message){
        System.out.println("Thông báo tới " + getFullName() + ": " + message);
    }
}
