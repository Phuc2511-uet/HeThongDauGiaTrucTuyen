package User;


import Observer.Observer;
import exceptions.AuthenticationException;

public abstract class User   {

    private String username;
    private String password;
    private String fullName;
    private int id;





    public User(int id,  String username, String password, String fullName) {
        this.id = id;


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



    public abstract void displayInfo();




    public void login(String inputPassword) throws AuthenticationException {
        if (!this.password.equals(inputPassword)) {
            // Ném ngoại lệ nếu sai mật khẩu
            throw new AuthenticationException("Mật khẩu không chính xác cho tài khoản: " + this.username);
        }
        System.out.println("Đăng nhập thành công!");
    }
}
