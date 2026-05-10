package Item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ItemManager implements Serializable {

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

    public void setItems(List<Item> items) {
        this.items = items;
    }

    // ===== THÊM ITEM =====
    public void addItem(Item item) {
        items.add(item);
    }

    // ===== LẤY ITEM THEO ID =====
    public Item getById(int id) {
        for (Item i : items) {
            if (i.getId() == id) {   // ✔ so sánh int
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
}