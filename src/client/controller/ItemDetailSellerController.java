package client.controller;

import client.network.ClientConnection;
import client.state.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class ItemDetailSellerController implements Observer {

    @FXML
    private Label lblId,lblName,lblPrice;

    private int itemId;

    @FXML
    private ImageView imgItem;

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

                lblId.setText(p[1]);
                lblName.setText(p[2].replace("_", " "));

                try {
                    double price = Double.parseDouble(p[3]);
                    lblPrice.setText(String.format("%,.0f $", price));
                } catch (Exception e) {
                    lblPrice.setText(p[3] + " $");
                }

                if (p.length > 5) {
                    String imageBase64 = p[5];

                    if (!imageBase64.equals("NONE")
                            && !imageBase64.equals("null")) {

                        showImage(imageBase64);
                    }
                }
            });
        }
    }

    private void showImage(String imageBase64) {
        try {
            byte[] imageBytes =
                    Base64.getDecoder().decode(imageBase64);

            Image image =
                    new Image(new ByteArrayInputStream(imageBytes));

            imgItem.setImage(image);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void backPage() {
        HomeSellerController.setPage("/client/view/resources/fxml/manageItem.fxml");
    }
}