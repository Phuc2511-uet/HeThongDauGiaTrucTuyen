package Item;


public class Vehicle extends Item {
    public Vehicle( String name, double price) {

        super( name, price);
    }
    @Override
    public void display() {
        System.out.println("[Vehicle] " + name+ " - Giá: " + price);
    }
}