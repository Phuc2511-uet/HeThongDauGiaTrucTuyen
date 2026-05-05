package Controller;
import NetWork.Client;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class MainFx extends Application {

    private Client client;

    @Override
    public void start(Stage stage) {

        stage.setTitle("Auction Client FX (Simple)");

        // 1. Khởi động client ở thread riêng
        startClient();

        // 2. UI rất đơn giản (không cần FXML cho bản test)
        stage.setOnCloseRequest(e -> {
            if (client != null) {
                client.disconnect();
            }
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    private void startClient() {
        new Thread(() -> {
            client = new Client();

            client.connect("localhost", 3636);





        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}