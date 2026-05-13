package Model.Auction;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class AuctionModel {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty status;

    public AuctionModel(int id, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.status = new SimpleStringProperty(status);
    }

    public int getId() { return id.get(); }
    public String getStatus() { return status.get(); }

    // Cần các getter trả về Property để TableView tự động cập nhật
    public SimpleIntegerProperty idProperty() { return id; }
    public SimpleStringProperty statusProperty() { return status; }
}