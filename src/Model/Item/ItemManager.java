package Model.Item;

import Controllers.Base.DatabaseManager; // Import DatabaseManager
import Model.AuctionManager.AuctionManager;
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
    public synchronized void addItem(Item item) {
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
    public synchronized void remove(int id) {
        try {
            // 1. Thực hiện xóa vĩnh viễn vật phẩm dưới Database MySQL
            DatabaseManager.deleteItem(id);
            System.out.println("Server >> Đã xóa thành công vật phẩm ID " + id + " dưới DB.");
        } catch (Exception e) {
            System.err.println("Server >> Lỗi khi thực hiện xóa vật phẩm dưới DB: " + e.getMessage());
        }

        // 2. Xóa vật phẩm khỏi danh sách bộ nhớ đệm (RAM) của Server
        items.removeIf(i -> i.getId() == id);
    }

    // ===== LẤY DANH SÁCH =====
    public List<Item> getItems() {
        return items;
    }
    public synchronized boolean updatePrice(int id, double newPrice) {
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

            //  nếu item đã có auction → bỏ qua (ẩn)
            if (AuctionManager.getInstance().getAuctionByItemId(i.getId()) == null) {
                sb.append(i.getId()).append(" ");
            }
        }

        return sb.toString().trim();
    }


    public synchronized Item createItem(String type, String name, double price, Seller seller) {

        // ===== VALIDATION =====
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Loại_vật_phẩm_không_hợp_lệ");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên_vật_phẩm_không_hợp_lệ");
        }

        if (seller == null) {
            throw new IllegalArgumentException("Seller_không_hợp_lệ");
        }

        // 👉 CHECK GIÁ (QUAN TRỌNG)
        if (price <= 0) {
            throw new IllegalArgumentException("Giá_phải_lớn_hơn_0");
        }

        // (tuỳ chọn) tránh giá quá nhỏ gây spam
        if (price < 100) {
            throw new IllegalArgumentException("Giá_tối_thiểu_là_100");
        }

        // (tuỳ chọn) giới hạn max để tránh lỗi hệ thống
        if (price > 1_000_000_000) {
            throw new IllegalArgumentException("Giá_quá_lớn");
        }

        // ===== FACTORY =====
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

        Item item = factory.CreateItem(name.trim(), price, seller);

        // ===== ID =====
        item.setId(count++);

        items.add(item);
        DatabaseManager.saveItem(item);

        return item;
    }
    public String getAvailableItemsBySeller(int sellerId) {

        StringBuilder sb = new StringBuilder("SELLER_AVAILABLE_ITEMS ");

        for (Item i : items) {

            if (i.getSeller() != null &&
                    i.getSeller().getId() == sellerId) {
                //format:    SELLER_AVAILABLE_ITEMS 30|xiaomi_car|1000.0
                if (AuctionManager.getInstance().getAuctionByItemId(i.getId()) == null) {
                    sb.append(" ").append(i.getId()).append("|").append(i.getName().replace(" ", "_")).append("|").append(i.getPrice());
                }
            }
        }

        return sb.toString().trim();
    }
}