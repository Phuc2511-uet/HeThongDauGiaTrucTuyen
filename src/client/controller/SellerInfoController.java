package client.controller;

import client.network.ClientConnection;
import client.state.Observer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SellerInfoController implements Observer {

    @FXML
    private Label lblUsername, lblRole, lblFullname, lblBalance;

    @FXML
    public void initialize() {
        ClientConnection.getInstance().removeObserver(this);
        ClientConnection.getInstance().addObserver(this);

        loadUserInfo();
    }

    private void loadUserInfo() {

        lblUsername.setText(
                ClientConnection.getInstance().getCurrentUsername()
        );

        lblRole.setText("Seller");

        lblFullname.setText(
                ClientConnection.getInstance().getCurrentFullname()
        );

        if (lblBalance != null) {
            lblBalance.setText(String.format("%,.2f $", ClientConnection.getInstance().getCurrentBalance())); // Hiển thị số dư ban đầu
        }
    }

    @Override
    public void update(String message) {
        if (message.equals("USER_DATA_CHANGED")) {
            javafx.application.Platform.runLater(() -> {
                lblUsername.setText(
                        ClientConnection.getInstance().getCurrentUsername()
                );

                lblFullname.setText(
                        ClientConnection.getInstance().getCurrentFullname()
                );
                // Cập nhật số dư mới khi có thông báo thay đổi
                if (lblBalance != null) {
                    lblBalance.setText(String.format("%,.2f $", ClientConnection.getInstance().getCurrentBalance()));
                }
            });
        }
    }
}