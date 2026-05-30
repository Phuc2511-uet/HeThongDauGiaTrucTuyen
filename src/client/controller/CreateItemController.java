package client.controller;

import client.network.ClientConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class CreateItemController {

    @FXML
    private TextField txtName,txtPrice;

    @FXML
    private ComboBox<String> cbType;

    @FXML
    private Button btnChooseImage;

    private String selectedImageBase64;

    @FXML
    private ImageView imgPreview;

    //type của item khi click button tạo vật phẩm
    @FXML
    public void initialize() {
        cbType.getItems().addAll("ART", "VEHICLE", "ELECTRONIC");
    }

    @FXML
    private void createItem() {
        try {
            String name = txtName.getText().trim();
            String priceStr = txtPrice.getText().trim();
            String type = cbType.getValue();

            if (name.isEmpty() || priceStr.isEmpty() || type == null) {
                showAlert("Cảnh báo", "Vui lòng nhập đủ thông tin");
                return;
            }

            double price = Double.parseDouble(priceStr);

            // Bẫy nhanh hạn mức tại ClientConnection để đỡ mất công gửi lên Server
            if (price <= 0) {
                showAlert( "Tạo thất bại", "Giá phải lớn hơn 0");
                return;
            }
            if (price > 1000000000) {
                showAlert("Tạo thất bại", "Giá không được vượt quá 1 tỷ");
                return;
            }

            if (selectedImageBase64 == null || selectedImageBase64.isBlank()) {
                showAlert("Thiếu ảnh", "Vui lòng chọn ảnh vật phẩm");
                return;
            }

            ClientConnection.getInstance().createItem(type, name, price, selectedImageBase64);
            HomeSellerController.setPage("/client/view/resources/fxml/manageItem.fxml");
        } catch (Exception e) {
            showAlert("Lỗi","Giá không hợp lệ");
        }
    }

    @FXML
    private void backPage() {
        HomeSellerController.setPage("/client/view/resources/fxml/manageItem.fxml");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void chooseImage() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Chọn ảnh vật phẩm");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );

            File file = chooser.showOpenDialog(null);
            if (file == null) return;

            if (file.length() > 300 * 1024) {
                showAlert("Ảnh quá lớn", "Ảnh phải nhỏ hơn 300KB");
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            selectedImageBase64 = Base64.getEncoder().encodeToString(bytes);

            Image image = new Image(file.toURI().toString());
            imgPreview.setImage(image);

            btnChooseImage.setVisible(false);

            showAlert("Thành công", "Đã chọn ảnh");

        } catch (Exception e) {
            showAlert("Lỗi", e.getMessage());
        }
    }
}
