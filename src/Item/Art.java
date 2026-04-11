package Item;


public class Art extends Item {
    public Art(String id, String name, double price) {
        super(id, name, price);
    }
    @Override
    public void display() {
        System.out.println("[Art] " + name+ " - Giá: " + price);
    }
}
