package Item;

import java.io.Serializable;

public abstract class Item implements Serializable {

    protected int id;
    protected String name;
    protected double price;

    private static int count = 0;

    public Item(String name, double price) {
        this.id = count++;   // 👈 tự sinh id giống User
        this.name = name;
        this.price = price;
    }

    // ===== GETTER =====
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    // ===== ABSTRACT =====
    public abstract void display();
}