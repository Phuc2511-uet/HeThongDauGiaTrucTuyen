package Controllers.Controller;

import View.Client.Client;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import java.io.IOException;

public class HomeAdminController {
    @FXML
    private ScrollPane mainContent;

    private static HomeAdminController instance;

    @FXML
    public void initialize() {
        instance = this;
    }

    public static void setPage(String fxmlPath) {
        if (instance == null || instance.mainContent == null) {
            System.err.println("HomeAdminController chưa được khởi tạo!");
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
    void showInfoAdmin() {
        setPage("/View/resources/fxml/adminInfo.fxml");
    }

    @FXML
    void showListUser() {
        setPage("/View/resources/fxml/adminListUser.fxml");
    }


    @FXML
    void showListItem() {
        setPage("/View/resources/fxml/adminListItem.fxml");
    }

    @FXML
    void showListAuction() {
        setPage("/View/resources/fxml/adminListAuction.fxml");
    }

    @FXML
    void Logout() {
        Client.getInstance().logOut();
    }
}