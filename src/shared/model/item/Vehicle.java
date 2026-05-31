package shared.model.item;

import shared.model.user.Seller; // Import Seller

public class Vehicle extends Item {
    private Seller seller; // Thêm thuộc tính seller



    // Constructor cũ, cập nhật để nhận seller
    public Vehicle(String name, double price, Seller seller) {
        super(name, price);
        this.seller = seller;
    }


    @Override
    public Seller getSeller() {
        return seller;
    }
}