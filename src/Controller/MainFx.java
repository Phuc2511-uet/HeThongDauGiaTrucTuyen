package Controller;
import NetWork.Client;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class MainFx extends Application {

    private void startClient() {
        new Thread(() -> {
            // DÙNG Singleton để connect
            Client.getInstance().connect("localhost", 3636);
        }).start();
    }

    private static Stage mainStage;

    public static void showHomeByRole(String role) {
        try {
            String fxmlPath = "";
            String title = "Hệ thống đấu giá -";
            switch (role.toUpperCase()) {
                case "BIDDER":
                    fxmlPath = "/fxml/homeBidder.fxml";
                    title += " Bidder";
                    break;
                case "SELLER":
                    fxmlPath = "/fxml/homeSeller.fxml";
                    title += " Seller";
                    break;
                case "ADMIN":
                    fxmlPath = "/fxml/homeAdmin.fxml";
                    title += " Admin";
                    break;
                default:
                    System.err.println("Vai trò không hợp lệ: " + role);
                    return;
            }

            FXMLLoader loader = new FXMLLoader(MainFx.class.getResource(fxmlPath));
            Parent root = loader.load();



            Scene scene = new Scene(root);
            mainStage.setResizable(true);

            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            mainStage.setX(bounds.getMinX());
            mainStage.setY(bounds.getMinY());
            mainStage.setWidth(bounds.getWidth());
            mainStage.setHeight(bounds.getHeight());

            mainStage.setScene(scene);
            mainStage.setTitle(title);

            mainStage.show();
            mainStage.setResizable(false);

            System.out.println("Đã chuyển sang màn hình: " + role);

        } catch (IOException e) {
            System.err.println("Lỗi khi tải file FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Hàm bổ trợ để quay lại màn hình Login
    public static void showLoginScene() {
        try {
            FXMLLoader loader = new FXMLLoader(MainFx.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            mainStage.hide();

            mainStage.setMaximized(false);
            mainStage.setFullScreen(false);
            mainStage.setResizable(false);

            mainStage.setWidth(900);
            mainStage.setHeight(600);

            Scene scene = new Scene(root);
            mainStage.setScene(scene);

            mainStage.centerOnScreen();
            mainStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //hàm bổ trợ quay về signin scene
    public static void showSignInScene() {
        try {
            FXMLLoader loader = new FXMLLoader(MainFx.class.getResource("/fxml/signin.fxml"));
            Parent root = loader.load();

            // 1. Tắt chế độ toàn màn hình và phóng to
            mainStage.setMaximized(false);
            mainStage.setFullScreen(false);

            // 2. Thiết lập Scene với kích thước chuẩn của file signin.fxml
            Scene scene = new Scene(root);
            mainStage.setScene(scene);

            // 3. Quan trọng: Ép cửa sổ co lại cho vừa khít với nội dung FXML mới
            mainStage.sizeToScene();

            // 4. Căn giữa lại màn hình
            mainStage.centerOnScreen();

            mainStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {

        mainStage = stage;
        // 1. Khởi động client ở thread riêng
        startClient();

        stage.setOnCloseRequest(e -> {
            // DÙNG Singleton để ngắt kết nối
            Client.getInstance().disconnect();
            Platform.exit();
            System.exit(0);
        });
        mainStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
        try {
            // Nạp file FXML từ thư mục resources/fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            // Thiết lập Scene với kích thước 900x600 như trong FXML
            Scene scene = new Scene(root, 900, 600);

            stage.setTitle("Hệ thống Đấu giá Trực tuyến");
            stage.setScene(scene);
            stage.setResizable(false); // Không cho phép kéo dãn cửa sổ để giữ giao diện đẹp
            stage.show();

            System.out.println("Giao diện đăng nhập đã được tải thành công.");

        } catch (Exception e) {
            // In ra lỗi chi tiết để debug (quan trọng khi bị null path hoặc lỗi controller)
            System.err.println("Lỗi nghiêm trọng khi khởi chạy ứng dụng:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}