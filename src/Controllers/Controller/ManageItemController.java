package Controllers.Controller;

import View.Client.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ManageItemController implements Observer {

    double itemPrice;

    @FXML
    private VBox itemContainer;

    @FXML
    public void initialize() {
        Client.getInstance().addObserver(this);
        loadItems();
    }

    private void loadItems() {
        itemContainer.getChildren().clear();
        Client.getInstance().getMyItems();
    }

    @Override
    public void update(String message) {
        Platform.runLater(() -> {

            if (message.startsWith("SELLER_AVAILABLE_ITEMS")) {
                itemContainer.getChildren().clear();
                String[] parts = message.split("\\s+");

                for (int i = 1; i < parts.length; i++) {
                    try {
                        int itemId;
                        String itemName;

                        // Kiểm tra xem Server đã gửi chuỗi định dạng mới "ID|Name|Price" chưa
                        if (parts[i].contains("|")) {
                            String[] itemData = parts[i].split("\\|");
                            itemId = Integer.parseInt(itemData[0]);
                            itemName = itemData[1].replace("_", " ");
                            itemPrice = Double.parseDouble(itemData[2]);
                        } else {
                            //  Nếu Server vẫn gửi dạng cũ (chỉ có ID), lấy ID làm Tên tạm thời
                            itemId = Integer.parseInt(parts[i]);
                            itemName = "Vật phẩm #" + itemId;
                        }

                        // Tạo và thêm card vào container giao diện
                        HBox card = createItemCard(itemId, itemName, itemPrice);
                        itemContainer.getChildren().add(card);

                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý phần tử vật phẩm tại index " + i + ": " + e.getMessage());
                    }
                }
            }
            if (message.startsWith("CREATE_AUCTION_SUCCESS")) {
                loadItems();
            }
            if (message.startsWith("CREATE_ITEM_SUCCESS")) {
                loadItems();
            }
            if (message.startsWith("SELLER_DELETE_ITEM_SUCCESS")) {
                loadItems();
            }
        });
    }

    //thêm vào danh sách
    private HBox createItemCard(int itemId, String itemName, double itemPrice) {
        HBox box = new HBox();
        box.setSpacing(20);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color:#1E293B;-fx-background-radius:15;");
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Hiển thị Tên vật phẩm (id vật phẩm)
        Label nameLabel = new Label(itemName + " (ID: " + itemId + ")");
        nameLabel.setStyle("-fx-text-fill:white;-fx-font-size:16;-fx-font-weight:bold;");

        // ép các nút hành động dạt về lề phải
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // nút Xem Chi Tiết (Sử dụng itemId của vật phẩm)
        Button detailBtn = new Button("Xem chi tiết");
        detailBtn.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-background-radius:10;-fx-padding: 8 15;");
        detailBtn.setOnAction(e -> openDetail(itemId));

        // nút Tạo Phiên Đấu Giá (Sử dụng itemId của vật phẩm)
        Button auctionBtn = new Button("Tạo phiên đấu giá");
        auctionBtn.setStyle("-fx-background-color:#16A34A;-fx-text-fill:white;-fx-background-radius:10;-fx-padding: 8 15;");
        auctionBtn.setOnAction(e -> createAuction(itemId, itemPrice));

        Button deleteBtn = new Button("Xóa");
        deleteBtn.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-background-radius:10;-fx-padding: 8 15;");
        deleteBtn.setOnAction(e -> deleteItem(itemId));

        // Gom các nút và nhãn vào HBox theo thứ tự
        box.getChildren().addAll(nameLabel, spacer, detailBtn, deleteBtn, auctionBtn);

        return box;
    }

    private void openDetail(int itemId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/resources/fxml/itemDetailSeller.fxml"));
            Parent root = loader.load();

            ItemDetailSellerController controller = loader.getController();
            controller.setItemId(itemId);

            HomeSellerController.setPageNode(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createAuction(int itemId, double itemPrice) {
        Client.getInstance().createAuction(String.valueOf(itemId), "0", itemPrice);
    }
    private void deleteItem(int itemId) {
        Client.getInstance().sellerDeleteItem(itemId);
    }

    @FXML
    private void openCreateItemPage() {
        HomeSellerController.setPage("/View/resources/fxml/createItem.fxml");
    }
}