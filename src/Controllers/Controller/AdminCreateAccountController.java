package Controllers.Controller;

import View.Client.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminCreateAccountController implements Observer {

    @FXML
    private TextField txtUsername, txtFullname;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private ComboBox<String> cbRole;

    @FXML
    public void initialize() {
        // Dọn dẹp Observer
        Client.getInstance().getObservers().clear();
        Client.getInstance().addObserver(this);

        // Nạp dữ liệu vào ComboBox
        cbRole.getItems().addAll("Bidder", "Seller");
        cbRole.setValue("Bidder");
    }

    @FXML
    private void handleCreate() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String fullname = txtFullname.getText().trim().replace(" ", "_"); // Format chuỗi gửi lên Server
        String role = cbRole.getValue().toUpperCase();

        if (username.isEmpty() || password.isEmpty() || fullname.isEmpty()) {
            showAlert("Cảnh báo", "Vui lòng điền đầy đủ các trường thông tin!");
            return;
        }

        // lệnh ADMIN_CREATE_ACCOUNT
        String cmd = "ADMIN_CREATE_ACCOUNT " + username + " " + password + " " + role + " " + fullname;
        Client.getInstance().send(cmd);
    }

    @FXML
    private void back() {
        HomeAdminController.setPage("/View/resources/fxml/adminListUser.fxml");
    }

    @Override
    public void update(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("ADMIN_CREATE_SUCCESS")) {
                showAlert("Thành công", "Đã cấp tài khoản thành công!");
                // Tự động chuyển hướng về lại trang danh sách user
                HomeAdminController.setPage("/View/resources/fxml/adminListUser.fxml");
            }
            else if (message.startsWith("ACCOUNT_FAILED")) {
                String reason = message.contains("USERNAME_EXISTS") ? "Tên tài khoản đã tồn tại!" : "Lỗi dữ liệu!";
                showAlert("Thất bại", "Không thể tạo tài khoản. Lý do: " + reason);
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}