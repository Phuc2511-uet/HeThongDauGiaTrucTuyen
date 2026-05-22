package Controllers.controller;

import Controllers.NetWork.Client;
import Model.Observer.Observer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminInfoController implements Observer {

    @FXML
    private Label lblFullname, lblUserName, lblRole;

    @FXML
    public void initialize() {
        // tránh add observer trùng
        Client.getInstance().removeObserver(this);
        // đăng ký observer
        Client.getInstance().addObserver(this);

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
        Client client = Client.getInstance();
        lblFullname.setText(client.getCurrentFullname());
        lblUserName.setText(client.getCurrentUsername());
        lblRole.setText("ADMIN");
    }
}