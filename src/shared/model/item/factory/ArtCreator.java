package shared.model.item.factory;
import shared.model.item.Art;
import shared.model.item.Item;
import shared.model.user.Seller; // Import Seller

public class ArtCreator extends ItemFactory{



    @Override
    public Item CreateItem(String name, double price, Seller seller) {
        return new Art(name, price, seller);
    }
}