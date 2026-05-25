package Controllers.controller;

import Controllers.NetWork.Client;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class AutoBidController {

    @FXML
    private Label lblAuctionId;
    @FXML
    private TextField txtMaxBid;
    @FXML
    private TextField txtIncrement;
    @FXML
    private Button btnCancel;

    private int auctionId;
    private double currentPrice; // Dùng để chặn dữ liệu nếu user nhập giá thấp hơn giá hiện tại

    /**
     * Nhận dữ liệu ID phiên đấu giá và Giá hiện tại truyền sang từ AuctionDetailController
     */
    public void setAuctionData(int auctionId, double currentPrice) {
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.lblAuctionId.setText(String.valueOf(auctionId));
    }

    @FXML
    private void handleActivateAutoBid() {
        String maxBidStr = txtMaxBid.getText().trim();
        String incrementStr = txtIncrement.getText().trim();

        // 1. Kiểm tra rỗng đầu vào
        if (maxBidStr.isEmpty() || incrementStr.isEmpty()) {
            showLocalAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền đầy đủ cả Giá trần và Bước giá!");
            return;
        }

        try {
            double maxBid = Double.parseDouble(maxBidStr);
            double increment = Double.parseDouble(incrementStr);

            // đk cơ bản
            if (maxBid <= 0 || increment <= 0) {
                showLocalAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Số tiền nhập vào phải lớn hơn 0!");
                return;
            }

            if (increment >= maxBid) {
                showLocalAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Bước giá tăng không thể lớn hơn hoặc bằng Giá trần!");
                return;
            }

            // min step 100
            if (increment < 100) {
                showLocalAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Bước giá tăng không được nhỏ hơn 100!");
                return;
            }

            // 3. KIỂM TRA LOGIC NGHIỆP VỤ ĐẤU GIÁ VỚI BIẾN CỦA CLIENT
            // Chặn nếu giá trần Auto Bid nhỏ hơn hoặc bằng giá hiện tại của món hàng
            if (maxBid <= currentPrice) {
                showLocalAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu",
                        String.format("Giá trần tối đa phải lớn hơn giá hiện tại của phiên (Hiện tại: %,.2f $)", currentPrice));
                return;
            }

            // Khớp hàm lấy số dư thực tế từ class Client của bạn: Client.getInstance().getCurrentBalance()
            double userBalance = Client.getInstance().getCurrentBalance();
            if (maxBid > userBalance) {
                showLocalAlert(Alert.AlertType.ERROR, "Số dư không đủ",
                        String.format("Giá trần không được vượt quá số dư tài khoản của bạn (Ví hiện tại: %,.2f $)", userBalance));
                return;
            }

            //  "REGISTER_AUTOBID <auction_id> <max_bid> <increment>"
            String message = "AUTO_BID " + auctionId + " " + maxBid + " " + increment;
            Client.getInstance().send(message);

            closeWindow();

        } catch (NumberFormatException e) {
            showLocalAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng chỉ nhập số hợp lệ, không chứa ký tự chữ hoặc ký hiệu lạ!");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private void showLocalAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}