package Item;


public class Electronic extends Item {
    public Electronic( String name, double price) {
        super(name, price);
    }
    @Override
    public void display() {
        System.out.println("[Electronic] " + name + " - Giá: " + price);
    }
}