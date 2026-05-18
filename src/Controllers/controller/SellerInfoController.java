package Controllers.controller;

import Controllers.NetWork.Client;
import Model.Observer.Observer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SellerInfoController implements Observer {

    @FXML
    private Label lblUsername, lblRole, lblFullname;

    @FXML
    public void initialize() {

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
    }

    @Override
    public void update(String message) {

        if (message.equals("USER_DATA_CHANGED")) {

            lblUsername.setText(
                    Client.getInstance().getCurrentUsername()
            );

            lblFullname.setText(
                    Client.getInstance().getCurrentFullname()
            );
        }
    }
}