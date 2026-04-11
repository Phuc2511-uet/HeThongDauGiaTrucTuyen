package Item;
import Base.Entity;

public class Electronic extends Item {
    public Electronic (String id, String name, double price) {
        super(id, name, price);
    }
    @Override
    public void display() {
        System.out.println("[Electronic] " + name + " - Giá: " + price);
    }
}