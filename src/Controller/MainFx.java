package Controller;
import NetWork.Client;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFx extends Application {

    private void startClient() {
        new Thread(() -> {
            // DÙNG Singleton để connect
            Client.getInstance().connect("localhost", 3636);
        }).start();
    }
    @Override
    public void start(Stage stage) throws Exception {

        // 1. Khởi động client ở thread riêng
        startClient();

        stage.setOnCloseRequest(e -> {
            // DÙNG Singleton để ngắt kết nối
            Client.getInstance().disconnect();
            Platform.exit();
            System.exit(0);
        });
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