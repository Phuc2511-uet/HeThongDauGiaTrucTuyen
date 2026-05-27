package shared.model.item.factory;
import shared.model.item.Art;
import shared.model.item.Item;
import shared.model.user.Seller; // Import Seller

public class ArtCreator extends ItemFactory{
    @Override
    public Item CreateItem(String name, double price){
        // Giả định rằng nếu không có Seller được cung cấp, item sẽ được tạo với seller là null
        // Hoặc có thể ném một ngoại lệ nếu Seller là bắt buộc
        return new Art(name,price, null);
    }

    @Override
    public Item CreateItem(String name, double price, Seller seller) {
        return new Art(name, price, seller);
    }
}