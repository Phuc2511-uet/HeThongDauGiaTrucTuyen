package Factory;
import Item.*;
import User.Seller; // Import Seller

public abstract class ItemFactory {
    // Phương thức cũ (có thể vẫn được sử dụng hoặc sẽ bị loại bỏ)
    public abstract Item CreateItem(String name,double price);

    // Phương thức mới để tạo Item với Seller
    public abstract Item CreateItem(String name, double price, Seller seller);
}
