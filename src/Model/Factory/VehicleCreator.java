package Model.Factory;
import Model.Item.Item;
import Model.Item.Vehicle;
import Model.User.Seller; // Import Seller

public class VehicleCreator extends ItemFactory{
    @Override
    public Item CreateItem(String name, double price){
        // Giả định rằng nếu không có Seller được cung cấp, item sẽ được tạo với seller là null
        // Hoặc có thể ném một ngoại lệ nếu Seller là bắt buộc
        return new Vehicle(name,price, null);
    }

    @Override
    public Item CreateItem(String name, double price, Seller seller) {
        return new Vehicle(name, price, seller);
    }
}