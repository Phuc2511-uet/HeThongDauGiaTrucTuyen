package Factory;
import Item.*;

public abstract class ItemFactory {
    public abstract Item CreateItem(String name,double price);

}
