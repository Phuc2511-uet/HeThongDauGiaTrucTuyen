package Controllers.Controller;

import View.Client.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Set;

public class AdminListAuctionController implements Observer {

    @FXML
    private VBox auctionContainer;

    // Bộ băm ngăn chặn hiện tượng trùng lặp dòng (Card) khi Thread nhận gói tin liên tục
    private final Set<Integer> loadedAuctionIds = new HashSet<>();

    @FXML
    public void initialize() {
        Client.getInstance().addObserver(this);
        loadAuctions();
    }

    private void loadAuctions() {
        auctionContainer.getChildren().clear();
        loadedAuctionIds.clear();
        Client.getInstance().send("GET_AUCTIONS");
    }

    @Override
    public void update(String message) {
        Platform.runLater(() -> {
            String cleanMessage = message.trim();

            if (cleanMessage.startsWith("SERVER >> ")) {
                cleanMessage = cleanMessage.substring("SERVER >> ".length()).trim();
            }

            if (cleanMessage.startsWith("LIST_AUCTION")) {
                auctionContainer.getChildren().clear();
                loadedAuctionIds.clear();

                String[] parts = cleanMessage.split("\\s+");
                if (parts.length <= 1) {
                    showEmptyMessage();
                    return;
                }

                for (int i = 1; i < parts.length; i++) {
                    String rawData = parts[i].trim();
                    if (rawData.isEmpty() || !rawData.contains("|")) continue;

                    try {
                        String[] pair = rawData.split("\\|");
                        int auctionId = Integer.parseInt(pair[0]);
                        Client.getInstance().send("GET_AUCTION_BY_ID " + auctionId);
                    } catch (Exception e) {
                        System.err.println("Lỗi phân rã ID phiên: " + e.getMessage());
                    }
                }
            }
            else if (cleanMessage.startsWith("AUCTION_DETAIL_SUCCESS")) {
                try {
                    String[] parts = cleanMessage.split("\\s+");
                    if (parts.length < 8) return;

                    int id = Integer.parseInt(parts[1]);
                    String itemName = parts[2].replace("_", " ");
                    double currentPrice = Double.parseDouble(parts[4]);
                    String seller = parts[5];
                    String status = parts[6].toUpperCase();
                    String bidder = parts[7].equalsIgnoreCase("NONE") ? "Chưa có" : parts[7];

                    if (loadedAuctionIds.contains(id)) return;
                    loadedAuctionIds.add(id);

                    HBox card = createAuctionCard(id, itemName, seller, status, bidder, currentPrice);
                    auctionContainer.getChildren().add(card);

                } catch (Exception e) {
                    System.err.println("Lỗi xử lý dựng thẻ phiên: " + e.getMessage());
                }
            }
            else if (cleanMessage.startsWith("CANCEL_AUCTION_SUCCESS")) {
                showAlert( "Thông báo", "Hủy bỏ phiên đấu giá thành công!");
                loadAuctions();
            }
            else if (cleanMessage.startsWith("RESTORE_AUCTION_SUCCESS")) {
                showAlert("Thông báo", "Khôi phục hoạt động phiên đấu giá thành công!");
                loadAuctions();
            }
            else if (cleanMessage.startsWith("STATUS_CHANGED")) {
                loadAuctions();
            }
            else if (cleanMessage.startsWith("ACTION_FAILED")) {
                showAlert("Lỗi tác vụ", "Hệ thống từ chối thực hiện yêu cầu.");
            }
        });
    }

    private HBox createAuctionCard(int id, String itemName, String seller, String status, String bidder, double currentPrice) {
        HBox box = new HBox();
        box.setSpacing(15);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 15;");
        box.setAlignment(Pos.CENTER_LEFT);

        VBox infoBox = new VBox(6);
        Label titleLabel = new Label("Vật phẩm: " + itemName + " (Mã Phiên: " + id + ")");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        Label detailsLabel = new Label("Người bán: " + seller + " | Hiện tại: " + currentPrice + "$ bởi [" + bidder + "]");
        detailsLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14;");
        infoBox.getChildren().addAll(titleLabel, detailsLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Hiển thị text trạng thái thô trực tiếp từ Enum của Server
        Label statusLabel = new Label("[" + status + "]");

        Button cancelBtn = new Button("Hủy phiên");
        Button restoreBtn = new Button("Khôi phục");

        // Bộ bảng màu phong cách Tailwind CSS UI tối giản, hiện đại
        String redBtnActive = "-fx-background-color: #DC2626; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 12;";
        String blueBtnActive = "-fx-background-color: #2563EB; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 12;";
        String btnDisabled = "-fx-background-color: #334155; -fx-text-fill: #64748B; -fx-background-radius: 8; -fx-padding: 6 12;";

        // Phân phối nghiệp vụ bật tắt nút bấm theo trạng thái phiên nhận được từ file Auction của bạn
        switch (status) {
            case "OPEN":
            case "RUNNING":
                statusLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 13; -fx-font-weight: bold;"); // Màu xanh lá cây

                cancelBtn.setDisable(false);
                cancelBtn.setStyle(redBtnActive);
                cancelBtn.setOnAction(e -> Client.getInstance().send("CANCEL_AUCTION " + id));

                restoreBtn.setDisable(true);
                restoreBtn.setStyle(btnDisabled);
                break;

            case "CANCELED":
                statusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13; -fx-font-weight: bold;"); // Màu đỏ nhạt

                cancelBtn.setDisable(true);
                cancelBtn.setStyle(btnDisabled);

                restoreBtn.setDisable(false);
                restoreBtn.setStyle(blueBtnActive);
                restoreBtn.setOnAction(e -> Client.getInstance().send("RESTORE_AUCTION " + id));
                break;

            default: // Các trạng thái kết thúc/đã giao dịch: FINISH, PAID
                statusLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13; -fx-font-weight: bold;"); // Màu xám

                cancelBtn.setDisable(true);
                cancelBtn.setStyle(btnDisabled);

                restoreBtn.setDisable(true);
                restoreBtn.setStyle(btnDisabled);
                break;
        }

        box.getChildren().addAll(infoBox, spacer, statusLabel, cancelBtn, restoreBtn);
        return box;
    }

    private void showEmptyMessage() {
        Label emptyLabel = new Label("Hiện tại chưa ghi nhận phiên đấu giá nào.");
        emptyLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 16; -fx-font-style: italic;");
        auctionContainer.getChildren().add(emptyLabel);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}