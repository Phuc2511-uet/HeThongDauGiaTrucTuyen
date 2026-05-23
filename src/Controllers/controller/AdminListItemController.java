package Controllers.controller;

import Controllers.NetWork.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Set;

public class AdminListItemController implements Observer {

    @FXML
    private VBox itemContainer;

    private final Set<Integer> loadedItemIds = new HashSet<>();

    @FXML
    public void initialize() {
        Client.getInstance().getObservers().removeIf(obs -> obs instanceof AdminListItemController);
        Client.getInstance().addObserver(this);
        loadItems();
    }

    private void loadItems() {
        loadedItemIds.clear();
        itemContainer.getChildren().clear();
        Client.getInstance().getItemIds();
    }

    @Override
    public void update(String message) {
        Platform.runLater(() -> {
            String cleanMessage = message.trim();

            if (cleanMessage.startsWith("ITEM_IDS")) {
                String[] parts = cleanMessage.split("\\s+");
                if (parts.length <= 1 || parts[1].trim().isEmpty()) {
                    System.out.println("Nhận gói tin ITEM_IDS rỗng từ Server, giữ nguyên giao diện.");
                    return;
                }
                itemContainer.getChildren().clear();
                loadedItemIds.clear();

                for (int i = 1; i < parts.length; i++) {
                    try {
                        int itemId = Integer.parseInt(parts[i]);
                        Client.getInstance().getItemById(itemId);
                    } catch (Exception e) {
                        System.err.println("Lỗi phân tích ID sản phẩm: " + e.getMessage());
                    }
                }
            }
            else if (cleanMessage.startsWith("ITEM_DETAIL")) {
                try {
                    String[] parts = cleanMessage.split("\\s+");

                    int id = Integer.parseInt(parts[1]);
                    String name = parts[2].replace("_", " ");
                    double price = Double.parseDouble(parts[3]);

                    // Đọc trạng thái YES/NO từ Server gửi về (Mặc định là NO nếu gói tin cũ thiếu)
                    boolean hasAuction = parts.length >= 5 && parts[4].equalsIgnoreCase("YES");
                    String type = "Đấu giá";

                    if (loadedItemIds.contains(id)) return;
                    loadedItemIds.add(id);

                    // Truyền thêm biến hasAuction vào hàm vẽ itemCard
                    HBox card = createItemCard(id, name, type, price, hasAuction);
                    itemContainer.getChildren().add(card);

                } catch (Exception e) {
                    System.err.println("Lỗi xử lý ITEM_DETAIL: " + e.getMessage());
                }
            }
            else if (cleanMessage.startsWith("DELETE_ITEM_SUCCESS")) {
                loadItems();
            }
        });
    }

    private HBox createItemCard(int itemId, String itemName, String itemType, double itemPrice, boolean hasAuction) {
        HBox box = new HBox();
        box.setSpacing(20);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color:#1E293B;-fx-background-radius:15;");
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label(itemName + " (ID: " + itemId + ") | Giá khởi điểm: " + itemPrice + "$");
        nameLabel.setStyle("-fx-text-fill:white;-fx-font-size:16;-fx-font-weight:bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("Xóa sản phẩm");

        // LOGIC KHÓA BUTTON VÀ THÊM LABEL TRẠNG THÁI
        if (hasAuction) {
            // 1. Nếu đã tạo phiên: Tạo Label trạng thái chữ màu vàng/cam bên trái nút xóa
            Label statusLabel = new Label("(Vật phẩm đã được tạo phiên đấu giá)");
            statusLabel.setStyle("-fx-text-fill:#F59E0B;-fx-font-size:14;-fx-font-style:italic;-fx-padding: 0 10 0 0;");

            // 2. Khóa cứng nút xóa và đổi style thành màu xám (disabled)
            deleteBtn.setDisable(true);
            deleteBtn.setStyle("-fx-background-color:#475569;-fx-text-fill:#94A3B8;-fx-background-radius:10;-fx-padding: 8 15;");

            // Thêm label trạng thái vào trước nút xóa
            box.getChildren().addAll(nameLabel, spacer, statusLabel, deleteBtn);
        } else {
            // Nếu chưa tạo phiên: Nút xóa hoạt động bình thường màu đỏ sáng
            deleteBtn.setDisable(false);
            deleteBtn.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-background-radius:10;-fx-padding: 8 15;");

            deleteBtn.setOnAction(e -> Client.getInstance().deleteItem(itemId));

            box.getChildren().addAll(nameLabel, spacer, deleteBtn);
        }
        return box;
    }
}