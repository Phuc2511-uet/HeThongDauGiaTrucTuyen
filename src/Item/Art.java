package Item;

import User.Seller; // Import Seller

public class Art extends Item {
    private Seller seller; // Thêm thuộc tính seller

    // Constructor mới nhận id và seller
    public Art(int id, String name, double price, Seller seller) {
        super(id, name, price);
        this.seller = seller;
    }

    // Constructor cũ, cập nhật để nhận seller
    public Art(String name, double price, Seller seller) {
        super(name, price);
        this.seller = seller;
    }

    @Override
    public void display() {
        System.out.println("[Art] " + name + " - Giá: " + price + " - Người bán: " + (seller != null ? seller.getUsername() : "N/A"));
    }

    @Override
    public Seller getSeller() {
        return seller;
    }
}
