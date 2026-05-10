package Controller;

import NetWork.Client;
import Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class BidderInfoController implements Observer {
    @FXML private Label lblFullname, lblBalance, lblUsername;

    @FXML
    public void initialize() {
        // 1. Đăng ký nhận thông báo từ Client
        NetWork.Client.getInstance().addObserver(this);
        // 2. Hiển thị dữ liệu đang có sẵn trong Client ngay lập tức
        refreshUI();
    }

    @Override
    public void update(String message) {
        if ("USER_DATA_CHANGED".equals(message)) {
            // Cập nhật lại nhãn nếu Server gửi tin nhắn thay đổi dữ liệu (ví dụ sau khi nạp tiền)
            javafx.application.Platform.runLater(this::refreshUI);
        }
    }

    private void refreshUI() {
        NetWork.Client client = NetWork.Client.getInstance();
        lblFullname.setText(client.getCurrentFullname());
        lblBalance.setText(String.format("%.2f $", client.getCurrentBalance()));
        lblUsername.setText(client.getCurrentUsername());
    }
}