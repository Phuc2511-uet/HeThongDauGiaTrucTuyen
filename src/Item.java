public abstract class Item extends Entity {
    protected String name;
    protected double price;
    //constructor
    public Item(String idItem,String name,double price){
        super(idItem);
        this.name = name;
        this.price = price;
    }
    
}
