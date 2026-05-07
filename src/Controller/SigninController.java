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
        // 1. Lấy dữ liệu và xóa khoảng trắng thừa
        String user = txtUsernameSingin.getText().trim();
        String pass = txtPasswordSignin.getText().trim();

        // 2. Kiểm tra tính hợp lệ
        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!");
            return;
        }

        // 3. Sử dụng Singleton Client để gửi dữ liệu
        System.out.println("Đang gửi yêu cầu đăng ký cho: " + user);

        // Theo cấu trúc hàm newAccount trong Client.java:
        // newAccount(String username, String password, String role, String fullname)
        // Ở đây mặc định role là "Bidder" và fullname tạm thời để giống username
        Client.getInstance().newAccount(user, pass, "Bidder", user);

        // 4. Thông báo cho người dùng
        // Lưu ý: Vì xử lý bất đồng bộ, thông báo này có nghĩa là "Đã gửi yêu cầu"
        showAlert(Alert.AlertType.INFORMATION, "Thành công",
                "Yêu cầu tạo tài khoản '" + user + "' đã được gửi tới hệ thống.");

        // (Tùy chọn) Tự động quay lại trang đăng nhập sau khi gửi
        // returnLogin(event);
    }

    // Hàm bổ trợ hiển thị thông báo nhanh
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
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
