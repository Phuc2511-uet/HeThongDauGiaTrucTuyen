package client.controller;

import client.network.ClientConnection;
import client.state.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AuctionWonController implements Observer {

    @FXML
    private VBox auctionContainer;

    @FXML
    public void initialize() {
        // Loại bỏ tận gốc tất cả các instance cũ của AuctionWonController để tránh bị nhân đôi alert
        ClientConnection.getInstance().getObservers().removeIf(obs -> obs instanceof AuctionWonController);
        ClientConnection.getInstance().addObserver(this);
        loadWonAuctions();
    }

    private void loadWonAuctions() {
        auctionContainer.getChildren().clear();
        ClientConnection.getInstance().send("GET_WON_AUCTIONS");
    }

    @Override
    public void update(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("WON_AUCTIONS_LIST")) {
                auctionContainer.getChildren().clear();
                String[] parts = message.split("\\s+");

                for (int i = 1; i < parts.length; i++) {
                    try {
                        String[] data = parts[i].split("\\|");
                        int auctionId = Integer.parseInt(data[0]);
                        String itemName = data[1].replace("_", " ");
                        double winPrice = Double.parseDouble(data[2]);

                        // Đọc trạng thái từ chuỗi Server trả về
                        String status = "FINISH";
                        if (data.length > 3) {
                            status = data[3].toUpperCase();
                        }

                        HBox card = createAuctionWonCard(auctionId, itemName, winPrice, status);
                        auctionContainer.getChildren().add(card);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            // Thanh toán thành công, Server gửi PAY_SUCCESS, giao diện tự động nạp lại dữ liệu
            if (message.startsWith("PAY_SUCCESS")) {
                String[] parts = message.split("\\s+");
                String auctionId = parts.length > 1 ? parts[1] : "";
                showAlert("Thành công", "Thanh toán hóa đơn phiên #" + auctionId + " thành công!");
                ClientConnection.getInstance().send("GET_CURRENT_USER"); // Cập nhật lại số dư hiển thị của Bidder
                loadWonAuctions(); // cập nhật trạng thái giao diện sang Đã thanh toán
            }

        });
    }

    private HBox createAuctionWonCard(int auctionId, String itemName, double winPrice, String status) {
        HBox box = new HBox();
        box.setSpacing(20);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color:#1E293B; -fx-background-radius:15; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox infoBox = new VBox();
        infoBox.setSpacing(5);

        Label titleLabel = new Label(itemName + " (ID Phiên: " + auctionId + ")");
        titleLabel.setStyle("-fx-text-fill:white; -fx-font-size:16; -fx-font-weight:bold;");

        Label priceLabel = new Label("Giá mua cuối: " + String.format("%,.2f", winPrice) + " $");
        priceLabel.setStyle("-fx-text-fill:#10B981; -fx-font-size:14; -fx-font-weight:bold;");

        infoBox.getChildren().addAll(titleLabel, priceLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(infoBox, spacer);

        Label statusBadge = new Label();
        Button payBtn = new Button("Thanh toán");

        // Tắt viền cửa scene
        statusBadge.setFocusTraversable(false);
        payBtn.setFocusTraversable(false);

        // ===== XỬ LÝ THEO TRẠNG THÁI THANH TOÁN =====
        if ("PAID".equals(status)) {
            //Nhãn trạng thái màu xanh cây
            statusBadge.setText(" Đã thanh toán ");
            statusBadge.setStyle("-fx-background-color:#065F46; -fx-text-fill:#34D399; -fx-padding: 8 15; -fx-background-radius: 10; -fx-font-weight: bold;");
            //Button màu xám, khóa tương tác (disable)
            payBtn.setStyle("-fx-background-color:#475569; -fx-text-fill:#94A3B8; -fx-background-radius:10; -fx-padding: 8 15; -fx-font-weight: bold; -fx-cursor: default;");
            payBtn.setDisable(true);
        } else {
            //CHƯA THANH TOÁN (FINISH) -> Nhãn trạng thái màu xám
            statusBadge.setText(" Chưa thanh toán ");
            statusBadge.setStyle("-fx-background-color:#334155; -fx-text-fill:#94A3B8; -fx-padding: 8 15; -fx-background-radius: 10; -fx-font-weight: bold;");
            //Button màu xanh cây, cho phép click thanh toán
            payBtn.setStyle("-fx-background-color:#10B981; -fx-text-fill:white; -fx-background-radius:10; -fx-padding: 8 15; -fx-font-weight: bold; -fx-cursor: hand;");
            payBtn.setDisable(false);

            payBtn.setOnAction(e -> {
                ClientConnection.getInstance().send("PAY " + auctionId);
            });
        }

        // dồn label và button về bên phải
        box.getChildren().addAll(statusBadge, payBtn);
        return box;
    }
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}