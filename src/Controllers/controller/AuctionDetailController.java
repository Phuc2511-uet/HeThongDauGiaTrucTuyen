package Controllers.controller;

import Controllers.NetWork.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AuctionDetailController implements Observer {
    @FXML
    private Label lblAuctionId, lblItemName, lblCurrentPrice, lblSeller, lblStatus,lblTitle,lblItemId;

    @FXML
    private TextField txtBidPrice;

    @FXML
    private Button btnBid;

    @FXML
    public void initialize() {
        // Đăng ký nhận thông báo từ Server
        Client.getInstance().addObserver(this);

        int id = Client.selectedAuctionId;
        lblTitle.setText("Thông tin chi tiết của phiên đấu giá #" + id);
        Client.getInstance().getAuctionById(id);
    }

    @Override
    public void update(String message) {
        // ===== LOAD CHI TIẾT AUCTION =====
        if (message.startsWith("AUCTION_DETAIL_SUCCESS")) {
            String[] parts = message.split("\\s+");
            if (parts.length > 7) {
                Platform.runLater(() -> {
                    lblAuctionId.setText(parts[1]);
                    lblItemName.setText(parts[2].replace("_", " "));
                    lblItemId.setText(parts[3]);
                    try {
                        double price = Double.parseDouble(parts[4]);
                        lblCurrentPrice.setText(
                                String.format("%,.0f $", price)
                        );
                    } catch (Exception e) {
                        lblCurrentPrice.setText(parts[4] + " $");
                    }
                    lblSeller.setText(parts[5]);
                    String statusStr = parts[7].toUpperCase();
                    if (statusStr.equals("0")) statusStr = "OPEN";
                    else if (statusStr.equals("1")) statusStr = "RUNNING";
                    else if (statusStr.equals("2")) statusStr = "FINISH";
                    else if (statusStr.equals("3")) statusStr = "PAID";
                    else if (statusStr.equals("4")) statusStr = "CANCELED";
                    lblStatus.setText(statusStr);
                    boolean canBid =
                            statusStr.equals("OPEN")
                                    || statusStr.equals("RUNNING");
                    btnBid.setDisable(!canBid);
                    if (!canBid) {
                        btnBid.setText("Không thể bid");
                    } else {
                        btnBid.setText("Đấu giá");
                    }
                    // ===== MÀU STATUS =====
                    if (statusStr.equals("RUNNING")
                            || statusStr.equals("OPEN")) {
                        lblStatus.setStyle("-fx-text-fill: #4ADE80;");
                    } else if (statusStr.equals("PAID")) {
                        lblStatus.setStyle("-fx-text-fill: #60A5FA;");
                    } else {
                        lblStatus.setStyle("-fx-text-fill: #FB7185;");
                    }
                });
            }
        }

        // ===== REFRESH GIÁ KHI BID =====
        else if (message.startsWith("NOTIFY")) {
            String[] parts = message.split("\\s+");
            int auctionId = Integer.parseInt(parts[1]);
            double newPrice = Double.parseDouble(parts[2]);
            // chỉ update đúng auction đang mở
            if (auctionId == Integer.parseInt(lblAuctionId.getText())) {
                Platform.runLater(() -> {
                    lblCurrentPrice.setText(
                            String.format("%,.0f $", newPrice)
                    );
                });
            }
        }

        // ===== REFRESH GIÁ AUTO BID =====
        else if (message.startsWith("AUTO_BID")) {
            String[] parts = message.split("\\s+");
            int auctionId = Integer.parseInt(parts[1]);
            double newPrice = Double.parseDouble(parts[2]);
            if (auctionId == Integer.parseInt(lblAuctionId.getText())) {
                Platform.runLater(() -> {
                    lblCurrentPrice.setText(
                            String.format("%,.0f $", newPrice)
                    );
                });
            }
        }
        else if (message.startsWith("STATUS_CHANGED")) {
            String[] parts = message.split("\\s+");
            int auctionId = Integer.parseInt(parts[1]);
            String status = parts[2];
            // chỉ update đúng auction đang xem
            if (auctionId == Integer.parseInt(lblAuctionId.getText())) {
                Platform.runLater(() -> {
                    lblStatus.setText(status);
                    boolean canBid =
                            status.equals("OPEN")
                                    || status.equals("RUNNING");
                    btnBid.setDisable(!canBid);
                    if (!canBid) {
                        btnBid.setText("Không thể bid");
                    } else {
                        btnBid.setText("Đấu giá");
                    }
                    // màu trạng thái
                    if (status.equals("RUNNING")
                            || status.equals("OPEN")) {
                        lblStatus.setStyle("-fx-text-fill: #4ADE80;");
                    } else if (status.equals("PAID")) {
                        lblStatus.setStyle("-fx-text-fill: #60A5FA;");
                    } else {
                        lblStatus.setStyle("-fx-text-fill: #FB7185;");
                    }
                });
            }
        }
    }

    @FXML
    private void handleBid() {
        try {
            String text = txtBidPrice.getText().trim();
            if (text.isEmpty()) {
                showAlert("Lỗi", "Vui lòng nhập giá bid!");
                return;
            }
            double price = Double.parseDouble(text);
            int auctionId = Integer.parseInt(lblAuctionId.getText());
            Client.getInstance().placeBid(auctionId, price);
            txtBidPrice.clear();
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Giá bid không hợp lệ!");
        } catch (Exception e) {
            showAlert("Lỗi", e.getMessage());
        }
    }

    // Hàm bổ trợ hiển thị lỗi nhanh
    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void backToList() {
        // Quan trọng: Hủy đăng ký Observer trước khi chuyển trang để giải phóng bộ nhớ
        Client.getInstance().removeObserver(this);
        // Quay lại trang danh sách
        HomeBidderController.setPage("/View/resources/fxml/auctionList.fxml");
    }
}