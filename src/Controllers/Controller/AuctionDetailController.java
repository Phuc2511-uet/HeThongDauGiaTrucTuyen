package Controllers.Controller;

import View.Client.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AuctionDetailController implements Observer {
    @FXML
    private Label lblAuctionId, lblItemName, lblCurrentPrice, lblSeller, lblStatus, lblTitle, lblItemId;

    @FXML
    private TextField txtBidPrice;

    @FXML
    private Button btnBid;

    //open scene autobid
    @FXML
    private Button btnRegisterAuto;

    // Cờ hiệu để biết chính mình vừa bấm nút đặt giá (Bid)
    private boolean isMyOwnBidAction = false;

    @FXML
    public void initialize() {
        // Đăng ký nhận thông báo từ Server
        Client.getInstance().addObserver(this);

        int id = Client.selectedAuctionId;
        lblTitle.setText("Thông tin chi tiết của phiên đấu giá #" + id);
        Client.getInstance().getAuctionById(id);

        if (Client.getInstance().isAutoBidActivatedForAuction(id)) {
            btnRegisterAuto.setDisable(true);
        }
    }

    @Override
    public void update(String message) {
        String cleanMessage = message.trim();

        // ===== LOAD CHI TIẾT AUCTION =====
        if (cleanMessage.startsWith("AUCTION_DETAIL_SUCCESS")) {
            String[] parts = cleanMessage.split("\\s+");
            if (parts.length > 7) {
                Platform.runLater(() -> {
                    lblAuctionId.setText(parts[1]);
                    lblItemName.setText(parts[2].replace("_", " "));
                    lblItemId.setText(parts[3]);
                    try {
                        double price = Double.parseDouble(parts[4]);
                        lblCurrentPrice.setText(String.format("%,.0f $", price));
                    } catch (Exception e) {
                        lblCurrentPrice.setText(parts[4] + " $");
                    }
                    lblSeller.setText(parts[5]);
                    String statusStr = parts[6].toUpperCase();
                    if (statusStr.equals("0")) statusStr = "OPEN";
                    else if (statusStr.equals("1")) statusStr = "RUNNING";
                    else if (statusStr.equals("2")) statusStr = "FINISH";
                    else if (statusStr.equals("3")) statusStr = "PAID";
                    else if (statusStr.equals("4")) statusStr = "CANCELED";
                    lblStatus.setText(statusStr);
                    boolean canBid = statusStr.equals("OPEN") || statusStr.equals("RUNNING");
                    btnBid.setDisable(!canBid);
                    if (!canBid) {
                        btnBid.setText("Không thể bid");
                    } else {
                        btnBid.setText("Đấu giá");
                    }

                    int currentAuctionId = Integer.parseInt(parts[1]);
                    boolean hasAutoBid = Client.getInstance().isAutoBidActivatedForAuction(currentAuctionId);
                    btnRegisterAuto.setDisable(!canBid || hasAutoBid);

                    // ===== MÀU STATUS =====
                    if (statusStr.equals("RUNNING") || statusStr.equals("OPEN")) {
                        lblStatus.setStyle("-fx-text-fill: #4ADE80;");
                    } else if (statusStr.equals("PAID")) {
                        lblStatus.setStyle("-fx-text-fill: #60A5FA;");
                    } else {
                        lblStatus.setStyle("-fx-text-fill: #FB7185;");
                    }
                });
            }
        }

        // ===== REFRESH GIÁ KHI BID (XỬ LÝ TRÁNH ĐÈ THÔNG BÁO) =====
        else if (cleanMessage.startsWith("NOTIFY")) {
            String[] parts = cleanMessage.split("\\s+");
            int auctionId = Integer.parseInt(parts[1]);
            double newPrice = Double.parseDouble(parts[2]);

            // Lấy username người vừa đặt giá từ gói tin (nếu Server có truyền về ở phần tử số 3)
            String bidderName = (parts.length > 3) ? parts[3] : "";

            if (!lblAuctionId.getText().isEmpty() && auctionId == Integer.parseInt(lblAuctionId.getText())) {
                Platform.runLater(() -> {
                    // 1. Luôn cập nhật lại tiền hiển thị trên UI cho khớp với hệ thống công khai
                    lblCurrentPrice.setText(String.format("%,.0f $", newPrice));

                    // 2. Kiểm tra xem người vừa bấm đặt giá có phải là chính mình không dựa vào Tên hoặc Cờ hiệu
                    boolean isMe = isMyOwnBidAction;
                    if (!isMe && Client.getInstance() != null && Client.getInstance().getCurrentUsername() != null) {
                        isMe = Client.getInstance().getCurrentUsername().equals(bidderName);
                    }

                    // 3. Nếu là ĐỐI THỦ đặt giá (không phải mình), hiển thị Toast
                    if (!isMe) {
                        // SỬA TẠI ĐÂY: Kiểm tra xem component giao diện còn tồn tại trên màn hình hay không
                        if (btnBid != null && btnBid.getScene() != null) {
                            Stage currentStage = (Stage) btnBid.getScene().getWindow();
                            NotificationToast.showSuccess(
                                    currentStage,
                                    "Giá Đấu Mới!",
                                    "Phiên #" + auctionId + " vừa được trả mức giá mới: " + String.format("%,.0f $", newPrice)
                            );
                        }
                    }

                    // 4. Reset lại cờ hiệu sau khi đã xử lý xong tin nhắn NOTIFY
                    isMyOwnBidAction = false;
                });
            }
        }

        // ===== AUTO BID KÍCH HOẠT THÀNH CÔNG =====
        else if (cleanMessage.startsWith("AUTO_BID_SUCCESS")) {
            String[] parts = cleanMessage.split("\\s+");

            // ĐỊNH DẠNG: parts[0]="AUTO_BID_SUCCESS", parts[1]=auctionId, parts[2]=username
            if (parts.length >= 3) {
                int responseAuctionId = Integer.parseInt(parts[1]);
                String targetUser = parts[2];
                String myUsername = Client.getInstance().getCurrentUsername();

                // LẤY mã ID phiên hiện tại đang hiển thị trên màn hình của Client này
                int currentId = Integer.parseInt(lblAuctionId.getText().trim());

                // ĐIỀU KIỆN QUYẾT ĐỊNH: Phải đúng phiên đấu giá này VÀ phải đúng là TÊN CỦA TÔI
                if (responseAuctionId == currentId && myUsername != null && myUsername.equals(targetUser)) {
                    Platform.runLater(() -> {
                        Client.getInstance().addActivatedAutoBidAuction(currentId);

                        // CHỈ khóa nút Auto Bid của người thực sự kích hoạt thành công
                        btnRegisterAuto.setDisable(true);

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thành công");
                        alert.setHeaderText(null);
                        alert.setContentText("Hệ thống đã kích hoạt chế độ Tự động Đấu giá thành công! Bạn không thể hoàn tác.");
                        alert.showAndWait();

                        // Đồng bộ lại dữ liệu chi tiết của phiên
                        Client.getInstance().getAuctionById(currentId);
                    });
                }
            }
        }

        // ===== AUTO BID KÍCH HOẠT THẤT BẠI =====
        else if (cleanMessage.startsWith("AUTO_BID_FAILED")) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi hệ thống");
                alert.setHeaderText(null);
                alert.setContentText("Kích hoạt chế độ Tự động Đấu giá thất bại! Vui lòng kiểm tra lại cấu hình hoặc số dư ví.");
                alert.showAndWait();
            });
        }

        // ===== THÔNG BÁO TỰ ĐỘNG TĂNG GIÁ (Cho cả phòng xem) =====
        else if (cleanMessage.startsWith("AUTO_BID")) {
            String[] parts = cleanMessage.split("\\s+");
            if (parts.length > 2) {
                int auctionId = Integer.parseInt(parts[1]);
                double newPrice = Double.parseDouble(parts[2]);
                if (!lblAuctionId.getText().isEmpty() && auctionId == Integer.parseInt(lblAuctionId.getText())) {
                    Platform.runLater(() -> {
                        lblCurrentPrice.setText(String.format("%,.0f $", newPrice));
                        Stage currentStage = (Stage) btnBid.getScene().getWindow();
                        NotificationToast.showSuccess(
                                currentStage,
                                "Hệ thống Tự động Đấu giá",
                                "Tự động tăng giá phiên #" + auctionId + " lên: " + String.format("%,.0f $", newPrice)
                        );
                    });
                }
            }
        }

        // ===== THAY ĐỔI TRẠNG THÁI PHIÊN =====
        else if (cleanMessage.startsWith("STATUS_CHANGED")) {
            String[] parts = cleanMessage.split("\\s+");
            int auctionId = Integer.parseInt(parts[1]);
            final String status = parts[2].toUpperCase();

            if (!lblAuctionId.getText().isEmpty() && auctionId == Integer.parseInt(lblAuctionId.getText())) {
                Platform.runLater(() -> {
                    lblStatus.setText(status);

                    boolean canBid = status.equals("OPEN") || status.equals("RUNNING");
                    btnBid.setDisable(!canBid);

                    if (!canBid) {
                        btnBid.setText("Không thể bid");
                    } else {
                        btnBid.setText("Đấu giá");
                    }

                    boolean hasAutoBid = Client.getInstance().isAutoBidActivatedForAuction(auctionId);
                    btnRegisterAuto.setDisable(!canBid || hasAutoBid);

                    if (status.equals("RUNNING") || status.equals("OPEN")) {
                        lblStatus.setStyle("-fx-text-fill: #4ADE80;");
                    } else if (status.equals("PAID")) {
                        lblStatus.setStyle("-fx-text-fill: #60A5FA;");
                    } else {
                        lblStatus.setStyle("-fx-text-fill: #FB7185;");
                    }

                    boolean isAdmin = false;
                    if (Client.getInstance() != null && Client.getInstance().getCurrentRole() != null) {
                        isAdmin = Client.getInstance().getCurrentRole().equalsIgnoreCase("ADMIN");
                    }

                    if (!isAdmin) {
                        Stage currentStage = (Stage) btnBid.getScene().getWindow();
                        if (status.equals("CANCELED")) {
                            NotificationToast.showSuccess(
                                    currentStage,
                                    "Phiên đấu giá bị HỦY!",
                                    "Phiên #" + auctionId + " đã bị Admin hủy bỏ."
                            );
                        } else if (status.equals("OPEN")) {
                            NotificationToast.showSuccess(
                                    currentStage,
                                    "Phiên đấu giá KHÔI PHỤC!",
                                    "Phiên #" + auctionId + " đã được Admin khôi phục."
                            );
                        }
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

            // ĐÁNH DẤU: Chính tôi là người chủ động thực hiện lệnh Bid này
            isMyOwnBidAction = true;

            Client.getInstance().placeBid(auctionId, price);

            // reload dữ liệu auction
            Client.getInstance().getAuctionById(auctionId);

            txtBidPrice.clear();
        } catch (NumberFormatException e) {
            // Thất bại thì hạ cờ xuống ngay
            isMyOwnBidAction = false;
            showAlert("Lỗi", "Giá bid không hợp lệ!");
        } catch (Exception e) {
            isMyOwnBidAction = false;
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
        Client.getInstance().removeObserver(this);
        // Quay lại trang danh sách
        HomeBidderController.setPage("/View/resources/fxml/auctionList.fxml");
    }

    @FXML
    private void handleSetUpAutoBid() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/View/resources/fxml/autoBid.fxml"));
            javafx.scene.Parent root = loader.load();

            AutoBidController popController = loader.getController();

            // Lấy ID phiên đấu giá từ Label
            int auctionId = Integer.parseInt(lblAuctionId.getText().trim());

            // LẤY GIÁ HIỆN TẠI: Loại bỏ ký tự '$', dấu phẩy phân tách nếu có để ép sang kiểu double
            String rawPrice = lblCurrentPrice.getText().trim()
                    .replace("$", "")
                    .replace(",", "")
                    .trim();
            double currentPrice = Double.parseDouble(rawPrice);

            // SỬA LỖI BÁO ĐỎ: Gọi chính xác hàm setAuctionData đã cập nhật bên AutoBidController
            popController.setAuctionData(auctionId, currentPrice);

            // 3. Khởi tạo một Stage mới
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.setTitle("Cấu hình Tự động Đấu giá - Phiên #" + auctionId);

            // Cấu hình MODALITY: Đóng băng màn hình nền phía sau.
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.initOwner(lblAuctionId.getScene().getWindow());

            // 4. Thiết lập Scene và hiển thị
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setResizable(false);
            popupStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Lỗi ứng dụng");
            alert.setHeaderText(null);
            alert.setContentText("Không thể hiển thị hộp cấu hình Auto Bid: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleShowBidHistory() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/View/resources/fxml/bidHistoryPopup.fxml"));
            javafx.scene.Parent root = loader.load();

            BidHistoryPopupController popupController = loader.getController();
            int auctionId = Integer.parseInt(lblAuctionId.getText().trim());
            popupController.setAuctionId(auctionId);

            Stage popupStage = new Stage();
            popupStage.setTitle("Lịch sử đấu giá - Phiên #" + auctionId);
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.initOwner(lblAuctionId.getScene().getWindow());
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setResizable(false);

            popupStage.setOnCloseRequest(event -> popupController.closePopup());

            popupStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi ứng dụng", "Không thể hiển thị lịch sử đấu giá: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowPriceChart(javafx.scene.input.MouseEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/View/resources/fxml/priceChartPopup.fxml")
            );
            javafx.scene.Parent root = loader.load();

            // 1. Lấy chính xác controller ra
            PriceChartPopupController controller = loader.getController();
            int auctionId = Integer.parseInt(lblAuctionId.getText().trim());

            // 2. KÍCH HOẠT TRUYỀN MÃ PHIÊN SANG VÀ PHÁT LỆNH GỌI SERVER
            controller.setAuctionId(auctionId);

            // 3. Khởi tạo cửa sổ popup độc lập
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.setTitle("Biểu đồ biến động giá - Phiên #" + auctionId);
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.initOwner(lblAuctionId.getScene().getWindow());
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setResizable(false);

            // 4. Đảm bảo khi bấm nút X tắt cửa sổ, hàm hủy Observer được thực thi sạch sẽ
            popupStage.setOnCloseRequest(e -> controller.closePopup());

            popupStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi ứng dụng", "Không thể hiển thị biểu đồ giá: " + e.getMessage());
        }
    }
}