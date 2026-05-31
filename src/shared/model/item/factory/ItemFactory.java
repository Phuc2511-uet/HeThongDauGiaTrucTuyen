package shared.model.item.factory;
import shared.model.item.Item;
import shared.model.user.Seller; // Import Seller

public abstract class ItemFactory {


    // Phương thức mới để tạo Item với Seller
    public abstract Item CreateItem(String name, double price, Seller seller);
}
