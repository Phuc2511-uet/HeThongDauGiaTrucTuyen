package Controllers.controller;

import Controllers.NetWork.Client;
import Model.Observer.Observer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class BidderInfoController implements Observer {
    @FXML private Label lblFullname, lblBalance, lblUsername;

    @FXML
    public void initialize() {
        // 1. Đăng ký nhận thông báo từ Client
        Client.getInstance().addObserver(this);
        // 2. Hiển thị dữ liệu đang có sẵn trong Client ngay lập tức
        refreshUI();
    }

    @Override
    public void update(String message) {
        if ("USER_DATA_CHANGED".equals(message)) {
            // Khi nhận tín hiệu, tự động cập nhật lại các Label trên màn hình
            javafx.application.Platform.runLater(() -> {
                refreshUI();
                System.out.println("Giao diện đã được cập nhật số dư mới!");
            });
        }
    }

    private void refreshUI() {
        Client client = Client.getInstance();
        lblFullname.setText(client.getCurrentFullname());
        lblBalance.setText(String.format("%.2f $", client.getCurrentBalance()));
        lblUsername.setText(client.getCurrentUsername());
    }

    @FXML
    public void DepositClick() {
        // Tạo ô nhập liệu nhanh (TextInputDialog)
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Hệ thống đấu giá");
        dialog.setHeaderText("Nạp tiền vào tài khoản");
        dialog.setContentText("Nhập số tiền muốn nạp ($):");

        java.util.Optional<String> result = dialog.showAndWait();

        result.ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount > 0) {
                    // Gửi lệnh qua Client
                    Client.getInstance().deposit(amount);
                } else {
                    showAlert("Lỗi","Số tiền phải lớn hơn 0!");
                }
            } catch (NumberFormatException e) {
                showAlert("Lỗi","Vui lòng nhập số tiền hợp lệ!");
            }
        });
    }

    // Hàm bổ trợ hiển thị lỗi nhanh
    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}