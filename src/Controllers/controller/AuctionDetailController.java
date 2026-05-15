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
        // Kiểm tra đúng tiền tố lệnh từ Server
        if (message.startsWith("AUCTION_DETAIL_SUCCESS")) {
            String[] parts = message.split("\\s+");

            // Kiểm tra độ dài mảng để tránh lỗi index
            if (parts.length >= 6) {
                Platform.runLater(() -> {
                    lblAuctionId.setText(parts[1]);
                    lblItemName.setText(parts[2].replace("_", " "));
                    lblItemId.setText(parts[3]);
                    try {
                        double price = Double.parseDouble(parts[4]);
                        lblCurrentPrice.setText(String.format("%,.0f $", price)); // Ví dụ: 22,000,000 $
                    } catch (Exception e) {
                        lblCurrentPrice.setText(parts[4] + " $");
                    }
                    lblSeller.setText(parts[5]);

                    // Xử lý màu sắc cho trạng thái (Tùy chọn thêm cho đẹp)
                    String statusStr = parts[6].toUpperCase();

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
                    }
                    else {
                        btnBid.setText("Đấu giá");
                    }

                    // 3. Đổi màu sắc theo trạng thái cho chuyên nghiệp
                    if (statusStr.equals("RUNNING") || statusStr.equals("OPEN")) {
                        lblStatus.setStyle("-fx-text-fill: #4ADE80;"); // Xanh lá
                    } else if (statusStr.equals("PAID")) {
                        lblStatus.setStyle("-fx-text-fill: #60A5FA;"); // Xanh dương
                    } else {
                        lblStatus.setStyle("-fx-text-fill: #FB7185;"); // Đỏ/Hồng
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