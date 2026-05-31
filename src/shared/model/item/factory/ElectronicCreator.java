package shared.model.item.factory;
import shared.model.item.Electronic;
import shared.model.item.Item;
import shared.model.user.Seller; // Import Seller

public class ElectronicCreator extends ItemFactory{


    @Override
    public Item CreateItem(String name, double price, Seller seller) {
        return new Electronic(name, price, seller);
    }
}