package Controller;

import NetWork.Client;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class SigninController {
    @FXML
    private TextField txtFullname;

    @FXML
    private TextField txtUsernameSingin;

    @FXML
    private PasswordField txtPasswordSignin;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    public void initialize() {
        // Nạp dữ liệu vào ComboBox
        roleComboBox.setItems(FXCollections.observableArrayList("Seller", "Bidder"));

        // Đặt giá trị mặc định nếu muốn
        roleComboBox.setValue("Bidder");
    }

    @FXML
    void signinAccount(ActionEvent event) {
        String user = txtUsernameSingin.getText().trim();
        String pass = txtPasswordSignin.getText().trim();
        String fullname = txtFullname.getText().trim();

        if (user.isEmpty() || pass.isEmpty() || fullname.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        String selectedRole = roleComboBox.getValue();

        // 1. Gửi lệnh tạo tài khoản
        if (selectedRole != null) {
            NetWork.Client.getInstance().newAccount(user, pass, selectedRole, fullname);
        }
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
