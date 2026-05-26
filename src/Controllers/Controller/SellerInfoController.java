package Controllers.Controller;

import View.Client.Client;
import Model.Observer.Observer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SellerInfoController implements Observer {

    @FXML
    private Label lblUsername, lblRole, lblFullname, lblBalance;

    @FXML
    public void initialize() {
        Client.getInstance().removeObserver(this);
        Client.getInstance().addObserver(this);

        loadUserInfo();
    }

    private void loadUserInfo() {

        lblUsername.setText(
                Client.getInstance().getCurrentUsername()
        );

        lblRole.setText("Seller");

        lblFullname.setText(
                Client.getInstance().getCurrentFullname()
        );

        if (lblBalance != null) {
            lblBalance.setText(String.format("%,.2f $", Client.getInstance().getCurrentBalance())); // Hiển thị số dư ban đầu
        }
    }

    @Override
    public void update(String message) {
        if (message.equals("USER_DATA_CHANGED")) {
            javafx.application.Platform.runLater(() -> {
                lblUsername.setText(
                        Client.getInstance().getCurrentUsername()
                );

                lblFullname.setText(
                        Client.getInstance().getCurrentFullname()
                );
                // Cập nhật số dư mới khi có thông báo thay đổi
                if (lblBalance != null) {
                    lblBalance.setText(String.format("%,.2f $", Client.getInstance().getCurrentBalance()));
                }
            });
        }
    }
}