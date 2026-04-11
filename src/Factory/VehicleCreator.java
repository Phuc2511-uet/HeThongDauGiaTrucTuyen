package Factory;
import Item.*;


public class VehicleCreator extends ItemFactory{
    @Override
    public Item CreateItem(String id,String name,double price){
        return new Vehicle(id,name,price);
    }
}