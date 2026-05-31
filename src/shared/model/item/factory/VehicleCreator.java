package shared.model.item.factory;
import shared.model.item.Item;
import shared.model.item.Vehicle;
import shared.model.user.Seller; // Import Seller

public class VehicleCreator extends ItemFactory{


    @Override
    public Item CreateItem(String name, double price, Seller seller) {
        return new Vehicle(name, price, seller);
    }
}