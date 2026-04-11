package Item;
import Base.Entity;

public class Vehicle extends Item {
    public Vehicle(String id, String name, double price) {

        super(id, name, price);
    }
    @Override
    public void display() {
        System.out.println("[Vehicle] " + name+ " - Giá: " + price);
    }
}