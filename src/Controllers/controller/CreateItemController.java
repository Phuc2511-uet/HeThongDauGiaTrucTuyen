package Controllers.controller;

import Controllers.NetWork.Client;
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
            String name = txtName.getText();
            double price = Double.parseDouble(txtPrice.getText());
            String type = cbType.getValue();
            if (name.isEmpty() || type == null) {
                showAlert("Vui lòng nhập đủ thông tin");
                return;
            }
            Client.getInstance().createItem(type,name,price);
            showAlert("Tạo item thành công");
            HomeSellerController.setPage("/View/resources/fxml/manageItem.fxml");
        } catch (Exception e) {
            showAlert("Giá không hợp lệ");
        }
    }

    @FXML
    private void backPage() {
        HomeSellerController.setPage("/View/resources/fxml/manageItem.fxml");
    }

    private void showAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}