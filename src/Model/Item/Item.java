package Model.Item;

import Model.User.Seller; // Import Seller
import java.io.Serializable;

public abstract class Item implements Serializable {

    protected int id;
    protected String name;
    protected double price;

    // Constructor mới nhận id
    public Item(int id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Constructor cũ, có thể dùng để tạo item mới chưa có id từ DB
    public Item(String name, double price) {
        // id sẽ được gán sau khi lưu vào DB hoặc được quản lý bởi ItemManager
        this.id = 0; // Gán ID mặc định 0 cho item mới chưa có ID từ DB
        this.name = name;
        this.price = price;
    }

    // ===== GETTER =====
    public int getId() {
        return id;
    }

    // Thay đổi quyền truy cập của setId() thành public
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    // ===== ABSTRACT =====
    public abstract void display();

    // Thêm phương thức trừu tượng để lấy Seller
    public abstract Seller getSeller();
}