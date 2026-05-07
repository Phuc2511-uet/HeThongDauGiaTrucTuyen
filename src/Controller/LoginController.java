package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

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
        try {
            // 1. Nạp file signin.fxml từ resources/fxml
            Parent signinRoot = FXMLLoader.load(getClass().getResource("/fxml/signin.fxml"));

            // 2. Tạo một Scene mới cho màn hình đăng ký
            Scene signinScene = new Scene(signinRoot);

            // 3. Lấy Stage (cửa sổ) hiện tại từ sự kiện nhấn nút
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 4. Thay đổi Scene và hiển thị
            stage.setScene(signinScene);
            stage.setTitle("Đăng ký tài khoản");
            stage.show();

            System.out.println("Đã chuyển sang màn hình Đăng ký.");

        } catch (IOException e) {
            System.err.println("Không tìm thấy file signin.fxml. Vui lòng kiểm tra lại đường dẫn!");
            e.printStackTrace();
        }
    }
}