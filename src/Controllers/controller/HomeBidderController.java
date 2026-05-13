package Controllers.controller;

import Controllers.NetWork.Client;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import java.io.IOException;

public class HomeBidderController {

    @FXML
    private ScrollPane mainContent;

    private static HomeBidderController instance;

    @FXML
    public void initialize() {
        instance = this;
    }

    public static void setPage(String fxmlPath) {
        if (instance == null || instance.mainContent == null) {
            System.err.println("HomeBidderController chưa được khởi tạo!");
            return;
        }
        try {
            var resource = instance.getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("Không tìm thấy file FXML tại: " + fxmlPath);
                return;
            }
            Parent fxml = FXMLLoader.load(resource);
            instance.mainContent.setContent(fxml);
        } catch (IOException e) {
            System.err.println("Lỗi khi tải trang: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    void showInfo() {
        setPage("/View/resources/fxml/bidderInfo.fxml");
    }

    @FXML
    void showAuctionList() {
        // 1. Chuyển trang trước
        setPage("/View/resources/fxml/auctionList.fxml");
        // 2. Gửi lệnh lấy dữ liệu (đảm bảo lệnh này khớp với Server)
        Client.getInstance().send("GET_AUCTIONS");
    }


    @FXML
    void showWonAuctions() {
        setPage("/View/resources/fxml/wonAuctions.fxml");
    }

    @FXML
    void Logout() {
        // 1. Báo cho Server để xóa connection (Lệnh này bạn đã viết trong Server)
        Client.getInstance().send("LOGOUT");

        // 2. Xóa các Observer để tránh rò rỉ bộ nhớ
        Client.getInstance().getObservers().clear();

        // 3. Chuyển về màn hình Đăng nhập (Dùng MainFx hoặc thay đổi Scene)
        // Ví dụ giả định bạn có hàm chuyển Scene chính:
        // MainFx.setRoot("/View/resources/fxml/login.fxml");
    }
}