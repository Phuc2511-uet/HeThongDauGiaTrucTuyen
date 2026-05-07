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
        String user = txtUsernameSingin.getText().trim();
        String pass = txtPasswordSignin.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // 1. Gửi lệnh tạo tài khoản
        Client.getInstance().newAccount(user, pass, "Bidder", user);

        // 2. Hiển thị thông báo nhanh cho người dùng
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.setContentText("Yêu cầu tạo tài khoản đã được gửi!");
        alert.showAndWait();

        // 3. Tự động gọi hàm returnLogin để quay về trang đăng nhập
        returnLogin(event);
    }

    // Hàm bổ trợ hiện Alert nhanh
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
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
