package Factory;
import Item.*;


public class VehicleCreator extends ItemFactory{
    @Override
    public Item CreateItem(String name,double price){
        return new Vehicle(name,price);
    }
}