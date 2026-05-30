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

public class AuctionDetailSellerController implements Observer {

    @FXML
    private Label lblId,lblItemId,lblBidder,lblItemName,lblPrice,lblStatus;

    private int auctionId;

    @FXML
    private ImageView imgItem;

    public void setAuctionId(int id) {
        this.auctionId = id;
        ClientConnection.getInstance().addObserver(this);
        ClientConnection.getInstance().getAuctionById(id);
    }

    @Override
    public void update(String message) {
        if (message.startsWith("AUCTION_DETAIL_SUCCESS")) {
            Platform.runLater(() -> {
                String[] p = message.split("\\s+");
                lblId.setText(p[1]);
                lblItemName.setText(p[2].replace("_", " "));
                lblItemId.setText(p[3]);
                try {
                    double price = Double.parseDouble(p[4]);
                    lblPrice.setText(String.format("%,.0f $", price));
                } catch (Exception e) {
                    lblPrice.setText(p[4] + " $");
                }
                lblStatus.setText(p[6]);
                if (p.length < 8 || p[8] == null || p[8].equalsIgnoreCase("null") || p[8].equalsIgnoreCase("NONE")) {
                    lblBidder.setText("Chưa có");
                } else {
                    lblBidder.setText(p[8]);
                }
                String imageBase64 = p[9];

                if (!imageBase64.equals("null")
                        && !imageBase64.equals("NONE")) {
                    showImage(imageBase64);
                }
            });
        }
    }

    private void showImage(String imageBase64) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);

            Image image = new Image(
                    new ByteArrayInputStream(imageBytes)
            );

            imgItem.setImage(image);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void backPage() {
        HomeSellerController.setPage("/client/view/resources/fxml/manageAuction.fxml");
    }
}