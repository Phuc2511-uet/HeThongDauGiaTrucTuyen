package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

public class LoginController {

    @FXML
    public TextField txtUsername;

    @FXML
    public PasswordField txtPassword;

    @FXML
    public void onLoginClick() {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        // Admin
        if (user.equals("admin") && pass.equals("123")) {
            System.out.println("Đăng nhập thành công!");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Sai tài khoản hoặc mật khẩu!");
            alert.show();
        }
    }
}