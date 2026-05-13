package Controllers.controller;

import Controllers.NetWork.Client;
import Model.Auction.AuctionModel;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;

public class AuctionListController implements Observer {
    @FXML
    private TableView<AuctionModel> tblAuctions;

    @FXML
    private TableColumn<AuctionModel, Integer> colId; // Chuyển sang Integer cho đồng bộ với ID

    @FXML
    private TableColumn<AuctionModel, String> colStatus;

    private final ObservableList<AuctionModel> auctionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        Client.getInstance().addObserver(this);

        // 1. Kết nối các cột với thuộc tính trong AuctionModel
        tblAuctions.setFixedCellSize(40);
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colId.setStyle("-fx-text-fill: #1E293B; -fx-font-weight: bold;");
        colStatus.setStyle("-fx-text-fill: #1E293B; -fx-font-weight: bold;");

        // 2. Gán danh sách rỗng ban đầu cho TableView
        tblAuctions.setItems(auctionList);

        // 3. Sự kiện Click dòng
        tblAuctions.setRowFactory(tv -> {
            TableRow<AuctionModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && (!row.isEmpty())) {
                    AuctionModel rowData = row.getItem();
                    Client.selectedAuctionId = rowData.getId();

                    // Trước khi chuyển trang, hủy đăng ký để tránh chồng chéo Observer
                    Client.getInstance().removeObserver(this);

                    HomeBidderController.setPage("/View/resources/fxml/auctionDetail.fxml");
                }
            });
            return row;
        });

        // 4. Gửi lệnh yêu cầu lấy danh sách mới nhất từ Server ngay khi mở trang
        Client.getInstance().send("GET_AUCTIONS");
    }

    @Override
    public void update(String message) {
        if (message.startsWith("LIST_AUCTION")) {
            Platform.runLater(() -> {
                auctionList.clear();
                String[] parts = message.split(" ");

                // Giả sử server gửi: LIST_AUCTION 1|Đang_diễn_ra 2|Sắp_kết_thúc
                for (int i = 1; i < parts.length; i++) {
                    try {
                        String[] data = parts[i].split("\\|");
                        if (data.length == 2) {
                            int id = Integer.parseInt(data[0]);
                            String status = data[1].replace("_", " ");
                            auctionList.add(new AuctionModel(id, status));
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi parse dữ liệu auction: " + parts[i]);
                    }
                }
            });
        }
    }
}