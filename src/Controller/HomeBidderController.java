package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import java.io.IOException;

public class HomeBidderController {

    @FXML
    private ScrollPane mainContent;

    private void setPage(String fxmlPath) {
        try {
            Parent fxml = FXMLLoader.load(getClass().getResource(fxmlPath));
            mainContent.setContent(fxml);
        } catch (IOException e) {
            System.err.println("Không tìm thấy file FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    void showInfo() {
        setPage("/fxml/bidderInfo.fxml");
    }

    @FXML
    void showAuctionList() {
        setPage("/fxml/auctionList.fxml");
    }

    @FXML
    void showWonAuctions() {
        setPage("/fxml/wonAuctions.fxml");
    }

    @FXML
    void Logout() {
        // Gọi hàm logOut trong Client để reset dữ liệu (FullName, Balance)
        // và xóa danh sách Observers cũ
        NetWork.Client.getInstance().logOut();
    }
}