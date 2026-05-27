package client.controller;

import client.network.ClientConnection;
import client.state.Observer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminInfoController implements Observer {

    @FXML
    private Label lblFullname, lblUserName, lblRole;

    @FXML
    public void initialize() {
        // tránh add observer trùng
        ClientConnection.getInstance().removeObserver(this);
        // đăng ký observer
        ClientConnection.getInstance().addObserver(this);

        refreshUI();
    }

    @Override
    public void update(String message) {
        if ("USER_DATA_CHANGED".equals(message)) {
            javafx.application.Platform.runLater(() -> {
                refreshUI();
                System.out.println("Admin info updated!");
            });
        }
    }

    private void refreshUI() {
        ClientConnection clientConnection = ClientConnection.getInstance();
        lblFullname.setText(clientConnection.getCurrentFullname());
        lblUserName.setText(clientConnection.getCurrentUsername());
        lblRole.setText("ADMIN");
    }
}
