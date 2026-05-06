package Item;


public class Art extends Item {
    public Art( String name, double price) {
        super( name, price);
    }
    @Override
    public void display() {
        System.out.println("[Art] " + name+ " - Giá: " + price);
    }
}
