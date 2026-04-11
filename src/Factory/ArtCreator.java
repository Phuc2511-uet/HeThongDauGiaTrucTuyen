package Factory;
import Item.*;


public class ArtCreator extends ItemFactory{
    @Override
    public Item CreateItem(String id,String name,double price){
        return new Art(id,name,price);
    }
}