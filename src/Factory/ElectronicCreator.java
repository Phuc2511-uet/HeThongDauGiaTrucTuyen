package Factory;
import Item.*;


public class ElectronicCreator extends ItemFactory{
    @Override
    public Item CreateItem(String name,double price){
        return new Electronic(name,price);
    }
}