package Factory;
import Item.*;


public class ElectronicCreator extends ItemFactory{
    @Override
    public Item CreateItem(String id,String name,double price){
        return new Electronic(id,name,price);
    }
}