package client.controller;

import client.network.ClientConnection;
import client.state.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ItemDetailSellerController implements Observer {

    @FXML
    private Label lblId,lblName,lblPrice;

    private int itemId;

    public void setItemId(int id) {
        this.itemId = id;
        ClientConnection.getInstance().addObserver(this);
        ClientConnection.getInstance().getItemById(id);
    }

    @Override
    public void update(String message) {
        if (message.startsWith("ITEM_DETAIL")) {
            Platform.runLater(() -> {
                String[] p = message.split("\\s+");
                // Hiển thị ID trực tiếp
                lblId.setText(p[1]);

                lblName.setText(p[2].replace("_", " "));

                // Định dạng hiển thị số tiền có dấu phẩy hàng nghìn
                try {
                    double price = Double.parseDouble(p[3]);
                    lblPrice.setText(String.format("%,.0f $", price));
                } catch (Exception e) {
                    lblPrice.setText(p[3] + " $");
                }
            });
        }
    }

    @FXML
    private void backPage() {
        HomeSellerController.setPage("/client/view/resources/fxml/manageItem.fxml");
    }
}