package Controllers.controller;

import Controllers.NetWork.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AuctionDetailSellerController implements Observer {

    @FXML
    private Label lblId,lblItemId,lblBidder,lblItemName,lblPrice,lblStatus;

    private int auctionId;

    public void setAuctionId(int id) {
        this.auctionId = id;
        Client.getInstance().addObserver(this);
        Client.getInstance().getAuctionById(id);
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
                if (p[7].equals("NONE")) {
                    lblBidder.setText("Chưa có");
                } else {
                    lblBidder.setText(p[7]);
                }
            });
        }
    }

    @FXML
    private void backPage() {
        HomeSellerController.setPage("/View/resources/fxml/manageAuction.fxml");
    }
}