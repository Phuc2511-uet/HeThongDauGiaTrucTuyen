package Controllers.controller;

import Controllers.NetWork.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ManageAuctionController implements Observer {
    @FXML
    private VBox auctionContainer;

    @FXML
    public void initialize() {
        Client.getInstance().addObserver(this);
        loadAuctions();
    }

    private void loadAuctions() {
        auctionContainer.getChildren().clear();
        Client.getInstance().getSellerAuctions();
    }

    @Override
    public void update(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("SELLER_AUCTIONS")) {
                auctionContainer.getChildren().clear();
                String[] parts = message.split("\\s+");
                for (int i = 1; i < parts.length; i++) {
                    try {
                        String[] data = parts[i].split("\\|");
                        int auctionId = Integer.parseInt(data[0]);
                        String itemName = data[1].replace("_", " ");
                        HBox card = createAuctionCard(auctionId, itemName);
                        auctionContainer.getChildren().add(card);
                    } catch (Exception e) {
                        System.out.println("Lỗi parse auction");
                    }
                }
            }
            if (message.startsWith("CREATE_AUCTION_SUCCESS")) {
                loadAuctions();
            }
        });
    }

    private HBox createAuctionCard(int auctionId, String itemName) {
        HBox box = new HBox();
        box.setSpacing(20);
        box.setPadding(new Insets(15));
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color:#1E293B;-fx-background-radius:15;");
        Label infoLabel = new Label("ID Phiên: " + auctionId + "   |   Vật phẩm: " + itemName);
        infoLabel.setStyle("-fx-text-fill:white;-fx-font-size:16;-fx-font-weight:bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button detailBtn = new Button("Xem chi tiết");
        detailBtn.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-background-radius:10;");
        detailBtn.setOnAction(e -> openDetail(auctionId));
        box.getChildren().addAll(infoLabel, spacer, detailBtn);
        return box;
    }
    private void openDetail(int auctionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/resources/fxml/auctionDetailSeller.fxml"));
            Parent root = loader.load();
            AuctionDetailSellerController controller = loader.getController();
            controller.setAuctionId(auctionId);
            HomeSellerController.setPageNode(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}