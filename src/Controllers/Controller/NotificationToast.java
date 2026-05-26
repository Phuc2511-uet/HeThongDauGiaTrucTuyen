package Controllers.controller;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NotificationToast {

    public static void showSuccess(Stage ownerStage, String title, String message) {
        Platform.runLater(() -> {
            Popup popup = new Popup();

            // 1. Dựng giao diện Box thông báo (Màu xanh lá nhạt chuẩn hiện đại)
            VBox toastBox = new VBox(5);
            toastBox.setPadding(new Insets(12, 20, 12, 20));
            toastBox.setAlignment(Pos.CENTER_LEFT);

            // CSS: Nền xanh nhạt, viền xanh đậm hơn chút, bo góc 8px, đổ bóng nhạt
            toastBox.setStyle(
                    "-fx-background-color: #DCFCE7; " + // Green 100
                            "-fx-border-color: #86EFAC; " +     // Green 300
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 8px; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);"
            );

            // Tiêu đề thông báo
            Label lblTitle = new Label(title);
            lblTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
            lblTitle.setTextFill(Color.web("#166534")); // Green 800

            // Nội dung thông báo
            Label lblMessage = new Label(message);
            lblMessage.setFont(Font.font("System", FontWeight.NORMAL, 12));
            lblMessage.setTextFill(Color.web("#1F2937")); // Gray 800
            lblMessage.setWrapText(true);
            lblMessage.setMaxWidth(300); // Giới hạn chiều rộng tránh tràn màn hình

            toastBox.getChildren().addAll(lblTitle, lblMessage);
            popup.getContent().add(toastBox);

            // 2. Tính toán vị trí hiển thị (Góc trên cùng bên phải của Cửa sổ ứng dụng)
            popup.setOnShown(e -> {
                double x = ownerStage.getX() + ownerStage.getWidth() - popup.getWidth() - 20;
                double y = ownerStage.getY() + 50; // Cách mép trên thanh tiêu đề một chút
                popup.setX(x);
                popup.setY(y);
            });

            // Hiển thị popup lên màn hình
            popup.show(ownerStage);

            // 3. Xử lý hiệu ứng tự biến mất sau 5 giây (Fade Out)
            toastBox.setOpacity(1.0);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(4.5), new KeyValue(toastBox.opacityProperty(), 1.0)), // Giữ nguyên 4.5s đầu
                    new KeyFrame(Duration.seconds(5.0), new KeyValue(toastBox.opacityProperty(), 0.0))  // 0.5s cuối mờ dần
            );

            timeline.setOnFinished(event -> popup.hide());
            timeline.play();
        });
    }
}