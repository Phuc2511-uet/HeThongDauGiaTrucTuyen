package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    void login(ActionEvent event) {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();
        System.out.println("Login click: " + user);
    }

    @FXML
    void signin(ActionEvent event) {
        System.out.println("Chuyển sang màn hình Đăng ký");
    }
}