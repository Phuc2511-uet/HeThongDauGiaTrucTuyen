package Controllers.controller;

import Controllers.NetWork.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AuctionDetailController implements Observer {
    @FXML
    private Label lblAuctionId, lblItemName, lblCurrentPrice, lblSeller, lblStatus,lblTitle;

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
        // Kiểm tra đúng tiền tố lệnh từ Server
        if (message.startsWith("AUCTION_DETAIL_SUCCESS")) {
            String[] parts = message.split("\\s+");

            // Kiểm tra độ dài mảng để tránh lỗi index
            if (parts.length >= 6) {
                Platform.runLater(() -> {
                    lblAuctionId.setText(parts[1]);
                    lblItemName.setText(parts[2].replace("_", " "));
                    lblCurrentPrice.setText(parts[3] + " $");
                    lblSeller.setText(parts[4]);

                    // Xử lý màu sắc cho trạng thái (Tùy chọn thêm cho đẹp)
                    String status = parts[5].replace("_", " ");
                    lblStatus.setText(status);
                    if (status.equalsIgnoreCase("ACTIVE")) {
                        lblStatus.setStyle("-fx-text-fill: #4ADE80;"); // Màu xanh lá
                    } else {
                        lblStatus.setStyle("-fx-text-fill: #FB7185;"); // Màu hồng/đỏ
                    }
                });
            }
        }
    }

    @FXML
    void backToList() {
        // Quan trọng: Hủy đăng ký Observer trước khi chuyển trang để giải phóng bộ nhớ
        Client.getInstance().removeObserver(this);

        // Quay lại trang danh sách
        HomeBidderController.setPage("/View/resources/fxml/auctionList.fxml");
    }
}