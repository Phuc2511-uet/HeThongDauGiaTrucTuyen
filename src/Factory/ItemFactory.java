package Factory;
import Item.*;

public class ItemFactory {
    public static Item createItem(String type, String id, String name, double price) {
        if (type == null) {
            return null;
        }
        switch (type.toLowerCase()) {
            case "electronic":
                return new Electronic(id, name, price);
            case "art":
                return new Art(id, name, price);
            case "vehicle":
                return new Vehicle(id, name, price);
            default:
                return null;
        }
    }
}
