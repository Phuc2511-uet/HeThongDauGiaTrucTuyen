package Item;
import Base.Entity;


public abstract class Item extends Entity {
    protected String name;
    protected double price;
    //constructor
    public Item(String id,String name,double price){
        super(id);
        this.name = name;
        this.price = price;
    }
}