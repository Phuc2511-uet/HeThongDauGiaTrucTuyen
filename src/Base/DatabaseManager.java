package Base;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import User.*;
import Item.*; // Import package chứa Item, Art, Electronic...
import Auction.*; // Import package chứa Auction
import AuctionManager.AuctionManager;
import Item.ItemManager;

public class DatabaseManager {

    /**
     * HÀM TỔNG: Load toàn bộ dữ liệu khi Server khởi động.
     * User -> Item -> Auction
     */
    public static void loadEverything() {
        System.out.println(">>> ĐANG KHỞI TẠO HỆ THỐNG TỪ AIVEN CLOUD...");

        // 1. Load toàn bộ User vào UserManager
        List<User> allUsers = loadAllUsers();
        UserManager.getInstance().setUsers(allUsers);
        System.out.println("- Đã nạp " + allUsers.size() + " người dùng.");

        // 2. Load toàn bộ Item vào ItemManager
        List<Item> allItems = loadAllItems(allUsers);
        ItemManager.getInstance().setItems(allItems);
        System.out.println("- Đã nạp " + allItems.size() + " vật phẩm.");

        // 3. Load toàn bộ Auction vào AuctionManager
        List<Auction> allAuctions = loadAllAuctions(allItems, allUsers);
        AuctionManager.getInstance().setAuctions(allAuctions);
        System.out.println("- Đã nạp " + allAuctions.size() + " phiên đấu giá.");

        System.out.println(">>> HỆ THỐNG ĐÃ SẴN SÀNG TRÊN RAM!");
    }

    // ============================================================
    // PHẦN 1: QUẢN LÝ USER
    // ============================================================

    public static List<User> loadAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String role = rs.getString("role");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String fullName = rs.getString("fullName");

                User u;
                if ("BIDDER".equalsIgnoreCase(role)) {
                    double balance = rs.getDouble("balance");
                    u = new Bidder(username, password, fullName, balance);
                } else {
                    u = new Seller(username, password, fullName);
                }
                list.add(u);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải người dùng: " + e.getMessage());
        }
        return list;
    }

    public static void saveUser(User user) {
        String sql = "INSERT INTO users (username, password, fullName, role, balance) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, (user instanceof Bidder) ? "BIDDER" : "SELLER");
            pstmt.setDouble(5, (user instanceof Bidder) ? ((Bidder) user).getBalance() : 0.0);
            pstmt.executeUpdate();
            System.out.println(">>> Đã lưu User: " + user.getUsername());
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu người dùng: " + e.getMessage());
        }
    }

    public static void updateUserState(User user) {
        String sql = "UPDATE users SET password = ?, fullName = ?, balance = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getPassword());
            pstmt.setString(2, user.getFullName());
            pstmt.setDouble(3, (user instanceof Bidder) ? ((Bidder) user).getBalance() : 0.0);
            pstmt.setString(4, user.getUsername());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật trạng thái người dùng: " + e.getMessage());
        }
    }

    // ============================================================
    // PHẦN 2: QUẢN LÝ ITEM
    // ============================================================

    public static List<Item> loadAllItems(List<User> allUsers) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String sellerName = rs.getString("seller_username");
                // Tìm Object User tương ứng để làm Seller
                User seller = allUsers.stream()
                        .filter(u -> u.getUsername().equals(sellerName))
                        .findFirst().orElse(null);

                if (seller instanceof Seller) {
                    Item item = new ConcreteItem(rs.getString("name"), rs.getDouble("base_price"), (Seller) seller);
                    item.setId(rs.getInt("item_id"));
                    list.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải vật phẩm: " + e.getMessage());
        }
        return list;
    }

    public static void saveItem(Item item) {
        String sql = "INSERT INTO items (name, base_price, seller_username) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, item.getName());
            pstmt.setDouble(2, item.getPrice());
            pstmt.setString(3, ((ConcreteItem) item).getSeller().getUsername());
            pstmt.executeUpdate();

            // Lấy ID tự sinh từ MySQL gán ngược lại cho Object
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) item.setId(rs.getInt(1));
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu vật phẩm: " + e.getMessage());
        }
    }

    // ============================================================
    // PHẦN 3: QUẢN LÝ AUCTION
    // ============================================================

    public static List<Auction> loadAllAuctions(List<Item> allItems, List<User> allUsers) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                // Tìm Item tương ứng
                Item item = allItems.stream()
                        .filter(i -> i.getId() == itemId)
                        .findFirst().orElse(null);

                String bidderName = rs.getString("highest_bidder_username");
                // Tìm User tương ứng làm Highest Bidder
                User bidder = allUsers.stream()
                        .filter(u -> u.getUsername().equals(bidderName))
                        .findFirst().orElse(null);

                if (item != null) {
                    Auction auction = new Auction(item, ((ConcreteItem) item).getSeller(), item.getPrice());
                    auction.setId(rs.getInt("auction_id"));
                    auction.setCurrentPrice(rs.getDouble("current_price"));
                    if (bidder instanceof Bidder) {
                        auction.setHighestBidder((Bidder) bidder);
                    }
                    list.add(auction);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải phiên đấu giá: " + e.getMessage());
        }
        return list;
    }

    public static void saveOrUpdateAuction(Auction auction) {
        // Sử dụng logic: Nếu chưa có ID thì INSERT, có rồi thì UPDATE
        String sql = (auction.getId() == 0)
                ? "INSERT INTO auctions (item_id, current_price, highest_bidder_username, status) VALUES (?, ?, ?, ?)"
                : "UPDATE auctions SET current_price = ?, highest_bidder_username = ?, status = ? WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (auction.getId() == 0) {
                pstmt.setInt(1, auction.getItem().getId());
                pstmt.setDouble(2, auction.getCurrentPrice());
                pstmt.setString(3, auction.getCurrentBidder() != null ? auction.getCurrentBidder().getUsername() : null);
                pstmt.setString(4, auction.getStatus().name()); // Sửa lỗi: Lưu trạng thái thực tế
            } else {
                pstmt.setDouble(1, auction.getCurrentPrice());
                pstmt.setString(2, auction.getCurrentBidder() != null ? auction.getCurrentBidder().getUsername() : null);
                pstmt.setString(3, auction.getStatus().name()); // Sửa lỗi: Lưu trạng thái thực tế
                pstmt.setInt(4, auction.getId());
            }

            pstmt.executeUpdate();
            if (auction.getId() == 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) auction.setId(rs.getInt(1));
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu/cập nhật phiên đấu giá: " + e.getMessage());
        }
    }
}
class ConcreteItem extends Item {
    private Seller seller;

    public ConcreteItem(String name, double price, Seller seller) {
        super(name, price);
        this.seller = seller;
    }

    public Seller getSeller() {
        return seller;
    }

    @Override
    public void display() {
        // Do nothing
    }
}