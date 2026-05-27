package client.controller;

import client.network.ClientConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class CreateItemController {

    @FXML
    private TextField txtName,txtPrice;

    @FXML
    private ComboBox<String> cbType;

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
            ClientConnection.getInstance().createItem(type, name, price);
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
}
