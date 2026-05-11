package Model.Item;

import Controllers.Base.DatabaseManager; // Import DatabaseManager
import Model.Factory.ArtCreator;
import Model.Factory.ElectronicCreator;
import Model.Factory.ItemFactory;
import Model.Factory.VehicleCreator;
import Model.User.Seller; // Import Seller

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

    public void setItems(List<Item> items) {
        this.items = items;
        // Cập nhật count để tránh trùng ID khi tải từ DB
        if (!items.isEmpty()) {
            this.count = items.stream().mapToInt(Item::getId).max().orElse(0) + 1;
        }
    }

    // ===== THÊM ITEM =====
    public void addItem(Item item) {
        items.add(item);
        DatabaseManager.saveItem(item); // Tự động lưu vào DB
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
        // TODO: Cần thêm logic xóa khỏi DB
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
        DatabaseManager.updateItem(item); // Tự động cập nhật vào DB
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
    public Item createItem(String type, String name, double price, Seller seller) { // Thêm Seller vào tham số

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

        Item item = factory.CreateItem(name, price, seller); // Gọi CreateItem với seller

        // gán ID tại đây
        item.setId(count++); // Giả sử setId đã được thêm lại hoặc Item có constructor với ID

        items.add(item);
        DatabaseManager.saveItem(item); // Tự động lưu vào DB

        return item;
    }
}