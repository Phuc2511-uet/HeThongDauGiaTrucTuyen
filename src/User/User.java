package User;

import java.io.Serializable;
import Observer.Observer;
import exceptions.AuthenticationException;

public abstract class User  implements Serializable,Observer {

    private String username;
    private String password;
    private String fullName;
    private int id;
    private static int count = 0;



    public User(  String username, String password, String fullName) {
        this.id = count;
        count ++;

        this.username = username;
        this.password = password;
        this.fullName = fullName;

    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPassword() { return password; }



    public abstract void displayInfo();

    @Override
    public void update(String message){
        System.out.println("Thông báo tới " + getFullName() + ": " + message);
    }

    public void login(String inputPassword) throws AuthenticationException {
        if (!this.password.equals(inputPassword)) {
            // Ném ngoại lệ nếu sai mật khẩu
            throw new AuthenticationException("Mật khẩu không chính xác cho tài khoản: " + this.username);
        }
        System.out.println("Đăng nhập thành công!");
    }
}
