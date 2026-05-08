package Factory;
import Item.*;


public class ArtCreator extends ItemFactory{
    @Override
    public Item CreateItem(String name,double price){
        return new Art(name,price);
    }
}