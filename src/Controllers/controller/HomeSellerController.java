package Controllers.controller;

import Controllers.NetWork.Client;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;

import java.io.IOException;

public class HomeSellerController {
    @FXML
    private ScrollPane mainContent;

    private static HomeSellerController instance;

    @FXML
    public void initialize() {
        instance = this;
    }
    // LOAD PAGE
    public static void setPage(String fxmlPath) {
        if (instance == null || instance.mainContent == null) {
            System.err.println("HomeSellerController chưa được khởi tạo!");
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
            instance.mainContent.setVvalue(0);
        } catch (IOException e) {
            System.err.println("Lỗi khi tải trang: " + fxmlPath);
            e.printStackTrace();
        }
    }
    @FXML
    void showInfoSeller() {
        setPage("/View/resources/fxml/sellerInfo.fxml");
    }

    @FXML
    void manageItem() {
        setPage("/View/resources/fxml/manageItem.fxml");
    }

    @FXML
    void manageAuction() {

        setPage("/View/resources/fxml/manageAuction.fxml");
    }

    @FXML
    void Logout() {

        Client.getInstance().logOut();
    }
}