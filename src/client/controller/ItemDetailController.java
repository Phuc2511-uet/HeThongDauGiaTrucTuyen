package client.controller;

import client.network.ClientConnection;
import client.state.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ItemDetailController implements Observer {
    public static int selectedItemId;

    @FXML
    private Label idLabel,nameLabel,priceLabel;

    @FXML
    public void initialize() {
        ClientConnection.getInstance().addObserver(this);
        ClientConnection.getInstance().getItemById(selectedItemId);
    }

    @Override
    public void update(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("ITEM_DETAIL")) {
                String[] parts = message.split("\\s+");
                idLabel.setText(parts[1]);
                nameLabel.setText(parts[2].replace("_", " "));
                priceLabel.setText(parts[3]);
            }
        });
    }

    @FXML
    private void back() {

        HomeSellerController.setPage(
                "/client/view/resources/fxml/manageItem.fxml"
        );
    }
}