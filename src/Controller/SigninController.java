package Controller;

import NetWork.Client;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class SigninController {
    @FXML
    private TextField txtUsernameSingin;

    @FXML
    private PasswordField txtPasswordSignin;

    @FXML
    void signinAccount(ActionEvent event) {
        // 1. Lấy dữ liệu từ giao diện
        String user = txtUsernameSingin.getText().trim();
        String pass = txtPasswordSignin.getText().trim();

        // 2. Kiểm tra nhanh (validation)
        if (user.isEmpty() || pass.isEmpty()) {
            System.out.println("Vui lòng không để trống!");
            return;
        }

        // 3. Gọi Client Singleton để gửi lệnh sang Sever
        // NEW_ACCOUNT <user> <pass> <role> <fullname>
        // Tạm thời để role là "Bidder" và fullname giống username
        Client.getInstance().newAccount(user, pass, "Bidder", user);

        System.out.println("Đã gửi yêu cầu đăng ký: " + user);

        // 4. (Tùy chọn) Quay lại màn hình đăng nhập sau khi bấm
        // returnLogin(event);
    }

    @FXML
    void returnLogin(ActionEvent event) {
        try {
            Parent returnRoot = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));

            Scene returnScene = new Scene(returnRoot,900, 600);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(returnScene);
            stage.setTitle("Đăng nhập hệ thống");
            stage.show();

        } catch (IOException e) {
            System.err.println("Không tìm thấy file login.fxml. Vui lòng kiểm tra lại đường dẫn!");
            e.printStackTrace();
        }
    }

}
