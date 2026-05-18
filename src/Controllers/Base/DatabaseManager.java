package Controllers.Base;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Model.Auction.Auction;
import Model.Item.Item;
import Model.User.Bidder;
import Model.User.Seller;
import Model.User.User;
import Model.User.UserManager;
import Model.AuctionManager.AuctionManager;
import Model.Item.ItemManager;

public class DatabaseManager {

    /**
     * HÀM TỔNG: Load toàn bộ dữ liệu khi Server khởi động.
     * Tối ưu: Dùng chung 1 Connection cho cả 3 hàm load để tăng tốc độ khởi động.
     */
    public static void loadEverything() {
        System.out.println(">>> ĐANG KHỞI TẠO HỆ THỐNG TỪ AIVEN CLOUD...");

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Load toàn bộ User
            List<User> allUsers = loadAllUsers(conn);
            UserManager.getInstance().setUsers(allUsers);
            System.out.println("- Đã nạp " + allUsers.size() + " người dùng.");

            // 2. Load toàn bộ Item
            List<Item> allItems = loadAllItems(conn, allUsers);
            ItemManager.getInstance().setItems(allItems);
            System.out.println("- Đã nạp " + allItems.size() + " vật phẩm.");

            // 3. Load toàn bộ Auction
            List<Auction> allAuctions = loadAllAuctions(conn, allItems, allUsers);
            AuctionManager.getInstance().setAuctions(allAuctions);
            System.out.println("- Đã nạp " + allAuctions.size() + " phiên đấu giá.");

            System.out.println(">>> HỆ THỐNG ĐÃ SẴN SÀNG TRÊN RAM!");

        } catch (SQLException e) {
            System.err.println(">>> LỖI KẾT NỐI DATABASE TỔNG: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // PHẦN 1: QUẢN LÝ USER
    // ============================================================

    public static List<User> loadAllUsers(Connection conn) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Lấy ID thật từ Database thay vì dùng biến đếm ảo
                // Nếu cột id của bạn tên khác (VD: user_id), hãy sửa ở đây
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String fullName = rs.getString("fullName");
                String role = rs.getString("role");

                User u;
                if ("BIDDER".equalsIgnoreCase(role)) {
                    double balance = rs.getDouble("balance");
                    u = new Bidder(id, username, password, fullName, balance);
                } else {
                    u = new Seller(id, username, password, fullName);
                }
                list.add(u);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải người dùng: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public static void saveUser(User user) {
        String sql = "INSERT INTO users (username, password, fullName, role, balance) VALUES (?, ?, ?, ?, ?)";
        // Thêm Statement.RETURN_GENERATED_KEYS để lấy lại ID thật do Database cấp
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, (user instanceof Bidder) ? "BIDDER" : "SELLER");
            pstmt.setDouble(5, (user instanceof Bidder) ? ((Bidder) user).getBalance() : 0.0);
            pstmt.executeUpdate();

            // Cập nhật ID thật cho User trên RAM
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }

            System.out.println(">>> Đã lưu User: " + user.getUsername() + " với ID: " + user.getId());
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu người dùng: " + e.getMessage());
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    // ============================================================
    // PHẦN 2: QUẢN LÝ ITEM
    // ============================================================

    public static List<Item> loadAllItems(Connection conn, List<User> allUsers) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT item_id, name, base_price, seller_username FROM items";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                String sellerName = rs.getString("seller_username");

                // So sánh an toàn: Xóa khoảng trắng thừa và không phân biệt hoa/thường
                User sellerUser = allUsers.stream()
                        .filter(u -> u.getUsername() != null && u.getUsername().trim().equalsIgnoreCase(sellerName.trim()))
                        .findFirst().orElse(null);

                // Ép kiểu an toàn (Safe Casting) để chống Crash
                Seller validSeller = null;
                if (sellerUser instanceof Seller) {
                    validSeller = (Seller) sellerUser;
                } else {
                    System.err.println("[CẢNH BÁO] Không tìm thấy Seller hợp lệ cho Item ID: " + itemId);
                }

                Item item = new ConcreteItem(itemId, rs.getString("name"), rs.getDouble("base_price"), validSeller);
                list.add(item);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải vật phẩm: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public static void saveItem(Item item) {
        String sql = "INSERT INTO items (name, base_price, seller_username) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, item.getName());
            pstmt.setDouble(2, item.getPrice());

            if (item.getSeller() != null) {
                pstmt.setString(3, item.getSeller().getUsername());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) item.setId(rs.getInt(1));
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu vật phẩm: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void updateItem(Item item) {
        String sql = "UPDATE items SET name = ?, base_price = ? WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getName());
            pstmt.setDouble(2, item.getPrice());
            pstmt.setInt(3, item.getId());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật vật phẩm: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // PHẦN 3: QUẢN LÝ AUCTION
    // ============================================================

    public static List<Auction> loadAllAuctions(Connection conn, List<Item> allItems, List<User> allUsers) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT auction_id, item_id, current_price, highest_bidder_username, status FROM auctions";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int auctionId = rs.getInt("auction_id");
                int itemId = rs.getInt("item_id");
                double currentPrice = rs.getDouble("current_price");
                String bidderName = rs.getString("highest_bidder_username");
                String statusString = rs.getString("status");

                Auction.Status status;
                try {
                    status = Auction.Status.valueOf(statusString.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    System.err.println("[CẢNH BÁO] Bỏ qua phiên đấu giá " + auctionId + " vì trạng thái lỗi: " + statusString);
                    continue; // Bỏ qua dòng lỗi này, nạp tiếp các dòng khác
                }

                Item item = allItems.stream()
                        .filter(i -> i.getId() == itemId)
                        .findFirst().orElse(null);

                Bidder validBidder = null;
                if (bidderName != null) {
                    User bidderUser = allUsers.stream()
                            .filter(u -> u.getUsername() != null && u.getUsername().trim().equalsIgnoreCase(bidderName.trim()))
                            .findFirst().orElse(null);

                    if (bidderUser instanceof Bidder) {
                        validBidder = (Bidder) bidderUser;
                    }
                }

                if (item != null) {
                    Auction auction = new Auction(auctionId, item, item.getSeller(), item.getPrice(), currentPrice, validBidder, status);
                    list.add(auction);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải phiên đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public static void saveOrUpdateAuction(Auction auction) {
        String sql = (auction.getId() == 0)
                ? "INSERT INTO auctions (item_id, current_price, highest_bidder_username, status) VALUES (?, ?, ?, ?)"
                : "UPDATE auctions SET current_price = ?, highest_bidder_username = ?, status = ? WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (auction.getId() == 0) {
                pstmt.setInt(1, auction.getItem().getId());
                pstmt.setDouble(2, auction.getCurrentPrice());
                pstmt.setString(3, auction.getCurrentBidder() != null ? auction.getCurrentBidder().getUsername() : null);
                pstmt.setString(4, auction.getStatus().name());
            } else {
                pstmt.setDouble(1, auction.getCurrentPrice());
                pstmt.setString(2, auction.getCurrentBidder() != null ? auction.getCurrentBidder().getUsername() : null);
                pstmt.setString(3, auction.getStatus().name());
                pstmt.setInt(4, auction.getId());
            }

            pstmt.executeUpdate();

            if (auction.getId() == 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) auction.setId(rs.getInt(1));
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu/cập nhật phiên đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class ConcreteItem extends Item {
    private final Seller seller;

    public ConcreteItem(int id, String name, double price, Seller seller) {
        super(id, name, price);
        this.seller = seller;
    }

    public ConcreteItem(String name, double price, Seller seller) {
        super(name, price);
        this.seller = seller;
    }

    @Override
    public Seller getSeller() {
        return seller;
    }

    @Override
    public void display() {
        // Do nothing
    }
}
