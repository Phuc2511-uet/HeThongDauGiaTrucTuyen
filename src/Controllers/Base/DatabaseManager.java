package Controllers.Base;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Model.Auction.Auction;
import Model.Item.Item;
import Model.User.*;
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
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String fullName = rs.getString("fullName");
                String role = rs.getString("role");

                User u;
                if ("BIDDER".equalsIgnoreCase(role)) {
                    double balance = rs.getDouble("balance");
                    // Đọc chuẩn cột mới từ DB
                    double reservedBalance = rs.getDouble("reserved_balance");

                    // Truyền chuẩn 6 tham số vào khuôn Bidder
                    u = new Bidder(id, username, password, fullName, balance, reservedBalance);
                } else if ("ADMIN".equalsIgnoreCase(role)) {
                    u = new Admin(id, username, password, fullName);
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

    // ===== DÁN ĐÈ HÀM SAVEUSER =====
    public static void saveUser(User user) {
        String sql = "INSERT INTO users (username, password, fullName, role, balance, reserved_balance) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, (user instanceof Bidder) ? "BIDDER" : "SELLER");
            pstmt.setDouble(5, (user instanceof Bidder) ? ((Bidder) user).getBalance() : 0.0);
            pstmt.setDouble(6, (user instanceof Bidder) ? ((Bidder) user).getReservedBalance() : 0.0); // Lưu reserved_balance
            pstmt.executeUpdate();

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

    // ===== DÁN ĐÈ HÀM UPDATEUSERSTATE =====
    public static void updateUserState(User user) {
        String sql = "UPDATE users SET password = ?, fullName = ?, balance = ?, reserved_balance = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getPassword());
            pstmt.setString(2, user.getFullName());
            pstmt.setDouble(3, (user instanceof Bidder) ? ((Bidder) user).getBalance() : 0.0);
            pstmt.setDouble(4, (user instanceof Bidder) ? ((Bidder) user).getReservedBalance() : 0.0); // Cập nhật reserved_balance
            pstmt.setString(5, user.getUsername());
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

    public static boolean deleteItem(int itemId) {

        String sql = "DELETE FROM items WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);

            int rows = pstmt.executeUpdate();

            return rows > 0;

        } catch (Exception e) {
            System.err.println("Lỗi khi xóa item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
        // 1. LẤY THÊM: start_time và end_time từ SQL
        String sql = "SELECT auction_id, item_id, current_price, highest_bidder_username, status, start_time, end_time FROM auctions";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int auctionId = rs.getInt("auction_id");
                int itemId = rs.getInt("item_id");
                double currentPrice = rs.getDouble("current_price");
                String bidderName = rs.getString("highest_bidder_username");
                String statusString = rs.getString("status");
                // Đọc dữ liệu thời gian kiểu long
                long startTime = rs.getLong("start_time");
                long endTime = rs.getLong("end_time");

                Auction.Status status;
                try {
                    status = Auction.Status.valueOf(statusString.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    System.err.println("[CẢNH BÁO] Bỏ qua phiên đấu giá " + auctionId + " vì trạng thái lỗi: " + statusString);
                    continue;
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
                    // 2. GỌI KHUÔN 9 THAM SỐ: Đưa thời gian lịch trình lên RAM
                    Auction auction = new Auction(auctionId, item, item.getSeller(), item.getPrice(), currentPrice, validBidder, status, startTime, endTime);

                    // 3. KHÔI PHỤC TIMER: Nếu phiên đang chạy (RUNNING), tự động kích hoạt đếm ngược tiếp
                    if (status == Auction.Status.RUNNING) {
                        auction.resumeAfterRestart();
                        System.out.println(">>> Đã khôi phục bộ đếm ngược chạy ngầm cho phiên ID: " + auctionId);
                    }

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
        // Thêm cột start_time, end_time vào cả câu lệnh INSERT và UPDATE
        String sql = (auction.getId() == 0)
                ? "INSERT INTO auctions (item_id, current_price, highest_bidder_username, status, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)"
                : "UPDATE auctions SET current_price = ?, highest_bidder_username = ?, status = ?, start_time = ?, end_time = ? WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (auction.getId() == 0) {
                pstmt.setInt(1, auction.getItem().getId());
                pstmt.setDouble(2, auction.getCurrentPrice());
                pstmt.setString(3, auction.getCurrentBidder() != null ? auction.getCurrentBidder().getUsername() : null);
                pstmt.setString(4, auction.getStatus().name());
                pstmt.setLong(5, auction.getStartTime());
                pstmt.setLong(6, auction.getEndTime());
            } else {
                pstmt.setDouble(1, auction.getCurrentPrice());
                pstmt.setString(2, auction.getCurrentBidder() != null ? auction.getCurrentBidder().getUsername() : null);
                pstmt.setString(3, auction.getStatus().name());
                pstmt.setLong(4, auction.getStartTime());
                pstmt.setLong(5, auction.getEndTime());
                pstmt.setInt(6, auction.getId()); // WHERE auction_id = ?
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
