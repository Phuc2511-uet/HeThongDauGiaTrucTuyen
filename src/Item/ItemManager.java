package Item;

import Factory.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ItemManager implements Serializable {
    private int count = 0;

    private static ItemManager instance;
    private List<Item> items;

    private ItemManager() {
        items = new ArrayList<>();
    }

    public static synchronized ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }

    // ===== THÊM ITEM =====
    public void addItem(Item item) {
        items.add(item);
    }

    // ===== LẤY ITEM THEO ID =====
    public Item getById(int id) {
        for (Item i : items) {
            if (i.getId() == id) {
                return i;
            }
        }
        return null;
    }

    // ===== KIỂM TRA TỒN TẠI =====
    public boolean exists(int id) {
        for (Item i : items) {
            if (i.getId() == id) {
                return true;
            }
        }
        return false;
    }

    // ===== XOÁ ITEM =====
    public void remove(int id) {
        items.removeIf(i -> i.getId() == id);
    }

    // ===== LẤY DANH SÁCH =====
    public List<Item> getItems() {
        return items;
    }
    public boolean updatePrice(int id, double newPrice) {
        Item item = getById(id);

        if (item == null) {
            return false;
        }

        item.setPrice(newPrice);
        return true;
    }
    public String getItemInfoAsString(int id) {

        Item i = getById(id);

        if (i == null) {
            return "ERROR ITEM NOT FOUND";
        }

        return "ITEM_DETAIL "
                + i.getId() + " "
                + i.getName().replace(" ", "_") + " "
                + i.getPrice();
    }
    public String getAllItemIdsAsString() {

        StringBuilder sb = new StringBuilder("ITEM_IDS ");

        for (Item i : items) {
            sb.append(i.getId()).append(" ");
        }

        return sb.toString().trim();
    }
    public Item createItem(String type, String name, double price) {

        ItemFactory factory;

        switch (type.toUpperCase()) {

            case "ELECTRONIC":
                factory = new ElectronicCreator();
                break;

            case "VEHICLE":
                factory = new VehicleCreator();
                break;

            case "ART":
                factory = new ArtCreator();
                break;

            default:
                throw new IllegalArgumentException("UNKNOWN ITEM TYPE");
        }

        Item item = factory.CreateItem(name, price);

        // gán ID tại đây
        item.setId(count++);

        items.add(item);

        return item;
    }
}