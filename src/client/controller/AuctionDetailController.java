package client.controller;

import client.network.ClientConnection;
import client.state.Observer;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class AuctionDetailController implements Observer {
    @FXML
    private Label lblAuctionId, lblItemName, lblCurrentPrice, lblSeller, lblStatus, lblTitle, lblItemId;

    @FXML
    private Label lblCountdown;

    @FXML
    private TextField txtBidPrice;

    @FXML
    private Button btnBid;

    @FXML
    private ImageView imgItem;

    //open scene autobid
    @FXML
    private Button btnRegisterAuto;

    // Cờ hiệu để biết chính mình vừa bấm nút đặt giá (Bid)
    private boolean isMyOwnBidAction = false;

    private Timeline countdownTimeline;
    private long remainingSeconds = 0;

    @FXML
    public void initialize() {
        // Đăng ký nhận thông báo từ Server
        ClientConnection.getInstance().addObserver(this);

        int id = ClientConnection.selectedAuctionId;
        lblTitle.setText("Thông tin chi tiết của phiên đấu giá #" + id);
        ClientConnection.getInstance().getAuctionById(id);
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

                    if (statusStr.equals("RUNNING")) {
                        if (parts.length > 7) {
                            try {
                                long endTimeMillis = Long.parseLong(parts[7]);
                                long currentTimestamp = System.currentTimeMillis();
                                long diffMillis = endTimeMillis - currentTimestamp;

                                if (diffMillis > 0) {
                                    this.remainingSeconds = diffMillis / 1000;
                                    lblCountdown.setVisible(true);
                                    startCountdown();
                                } else {
                                    stopCountdown();
                                    lblCountdown.setVisible(false);
                                }
                            } catch (Exception ignored) {}
                        }
                    } else {
                        stopCountdown();
                        lblCountdown.setVisible(false);
                    }

                    boolean canBid = statusStr.equals("OPEN") || statusStr.equals("RUNNING");
                    btnBid.setDisable(!canBid);
                    if (!canBid) {
                        btnBid.setText("Không thể bid");
                    } else {
                        btnBid.setText("Đấu giá");
                    }

                    int currentAuctionId = Integer.parseInt(parts[1]);
                    boolean hasAutoBid = ClientConnection.getInstance().isAutoBidActivatedForAuction(currentAuctionId);
                    btnRegisterAuto.setDisable(!canBid || hasAutoBid);

                    if (parts.length > 9) {

                        String imageBase64 = parts[9];

                        if (!imageBase64.equalsIgnoreCase("NONE")
                                && !imageBase64.equalsIgnoreCase("null")) {

                            showImage(imageBase64);
                        }
                    }

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
                    if (!isMe && ClientConnection.getInstance() != null && ClientConnection.getInstance().getCurrentUsername() != null) {
                        isMe = ClientConnection.getInstance().getCurrentUsername().equals(bidderName);
                    }

                    // 3. Nếu là ĐỐI THỦ đặt giá (không phải mình), hiển thị Toast
                    if (!isMe) {
                        Stage currentStage = (Stage) btnBid.getScene().getWindow();
                        NotificationToast.showSuccess(
                                currentStage,
                                "Giá Đấu Mới!",
                                "Phiên #" + auctionId + " vừa được trả mức giá mới: " + String.format("%,.0f $", newPrice)
                        );
                    }

                    // 4. Reset lại cờ hiệu sau khi đã xử lý xong tin nhắn NOTIFY
                    isMyOwnBidAction = false;
                });
            }
        }

        // ===== REFRESH GIÁ AUTO BID =====
        else if (cleanMessage.startsWith("AUTO_BID")) {
            // TRƯỜNG HỢP 1: Server báo đăng ký cấu hình thành công
            if (cleanMessage.contains("SUCCESS")) {
                Platform.runLater(() -> {
                    int currentId = Integer.parseInt(lblAuctionId.getText().trim());
                    ClientConnection.getInstance().addActivatedAutoBidAuction(currentId);

                    btnRegisterAuto.setDisable(true);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thành công");
                    alert.setHeaderText(null);
                    alert.setContentText("Hệ thống đã kích hoạt chế độ Tự động Đấu giá thành công! Bạn không thể hoàn tác.");
                    alert.showAndWait();

                    // Đồng bộ lại dữ liệu chi tiết của phiên sau khi kích hoạt thành công
                    int id = ClientConnection.selectedAuctionId;
                    ClientConnection.getInstance().getAuctionById(id);
                });
            }
            // TRƯỜNG HỢP 2: Server báo lỗi khi đăng ký (Ví dụ: số dư hoặc phiên lỗi)
            else if (cleanMessage.contains("FAILED")) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lỗi hệ thống");
                    alert.setHeaderText(null);
                    alert.setContentText("Kích hoạt chế độ Tự động Đấu giá thất bại! Vui lòng kiểm tra lại cấu hình hoặc số dư ví.");
                    alert.showAndWait();
                });
            }
            // TRƯỜNG HỢP 3: Gói tin notify nhảy giá tự động (AUTO_BID <auctionId> <newPrice>)
            else {
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
        }

        // ===== THAY ĐỔI TRẠNG THÁI PHIÊN =====
        else if (cleanMessage.startsWith("STATUS_CHANGED")) {
            String[] parts = cleanMessage.split("\\s+");
            int auctionId = Integer.parseInt(parts[1]);
            final String status = parts[2].toUpperCase();

            if (!lblAuctionId.getText().isEmpty() && auctionId == Integer.parseInt(lblAuctionId.getText())) {
                Platform.runLater(() -> {
                    lblStatus.setText(status);

                    if ("OPEN".equalsIgnoreCase(status)) {
                        stopCountdown();
                        lblCountdown.setVisible(false);
                    }
                    else if ("RUNNING".equalsIgnoreCase(status)) {
                        if (parts.length > 3) {
                            try {
                                long endTimeMillis = Long.parseLong(parts[3]);
                                long currentTimestamp = System.currentTimeMillis();
                                long diffMillis = endTimeMillis - currentTimestamp;

                                if (diffMillis > 0) {
                                    this.remainingSeconds = diffMillis / 1000;
                                    lblCountdown.setVisible(true);
                                    startCountdown();
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    else {
                        stopCountdown();
                        lblCountdown.setVisible(false);
                    }

                    boolean canBid = status.equals("OPEN") || status.equals("RUNNING");
                    btnBid.setDisable(!canBid);

                    if (!canBid) {
                        btnBid.setText("Không thể bid");
                    } else {
                        btnBid.setText("Đấu giá");
                    }

                    boolean hasAutoBid = ClientConnection.getInstance().isAutoBidActivatedForAuction(auctionId);
                    btnRegisterAuto.setDisable(!canBid || hasAutoBid);

                    if (status.equals("RUNNING") || status.equals("OPEN")) {
                        lblStatus.setStyle("-fx-text-fill: #4ADE80;");
                    } else if (status.equals("PAID")) {
                        lblStatus.setStyle("-fx-text-fill: #60A5FA;");
                    } else {
                        lblStatus.setStyle("-fx-text-fill: #FB7185;");
                    }

                    // SỬA LỖI BIẾN Ở ĐÂY: Kiểm tra quyền ADMIN để chặn thông báo nếu muốn
                    boolean isAdmin = false;
                    if (ClientConnection.getInstance() != null && ClientConnection.getInstance().getCurrentRole() != null) {
                        isAdmin = ClientConnection.getInstance().getCurrentRole().equalsIgnoreCase("ADMIN");
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

            ClientConnection.getInstance().placeBid(auctionId, price);

            // reload dữ liệu auction
            ClientConnection.getInstance().getAuctionById(auctionId);

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
        // nếu ko gọi hàm stop thì sẽ làm rò rỉ bộ đếm chạy ngầm vô hạn
        stopCountdown();

        ClientConnection.getInstance().removeObserver(this);
        // Quay lại trang danh sách
        HomeBidderController.setPage("/client/view/resources/fxml/auctionList.fxml");
    }

    @FXML
    private void handleSetUpAutoBid() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/resources/fxml/autoBid.fxml"));
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
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/resources/fxml/bidHistoryPopup.fxml"));
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
    private void handleShowPriceChart() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/resources/fxml/priceChartPopup.fxml"));
            javafx.scene.Parent root = loader.load();

            PriceChartPopupController popupController = loader.getController();
            int auctionId = Integer.parseInt(lblAuctionId.getText().trim());
            popupController.setAuctionId(auctionId);

            Stage popupStage = new Stage();
            popupStage.setTitle("Price chart - Auction #" + auctionId);
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.initOwner(lblAuctionId.getScene().getWindow());
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setResizable(false);

            popupStage.setOnCloseRequest(event -> popupController.closePopup());

            popupStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Loi ung dung", "Khong the hien thi bieu do gia: " + e.getMessage());
        }
    }

    private void showImage(String imageBase64) {
        try {
            byte[] imageBytes =
                    Base64.getDecoder().decode(imageBase64);

            Image image =
                    new Image(new ByteArrayInputStream(imageBytes));

            imgItem.setImage(image);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========  HÀM BỔ TRỢ TIMELINE =========
    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        // 1. Tạo một hàm Runnable nội bộ để định dạng và hiển thị thời gian
        Runnable renderTime = () -> {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;
            lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        };

        // Gán luôn thời gian lên UI(để ko bị khựng)
        renderTime.run();

        // 2. Thiết lập Timeline chạy lặp lại sau mỗi giây
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                renderTime.run(); // Cập nhật lại UI sau khi trừ giây
            } else {
                lblCountdown.setText("00:00:00");
                countdownTimeline.stop();
                lblCountdown.setVisible(false);
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }
}
