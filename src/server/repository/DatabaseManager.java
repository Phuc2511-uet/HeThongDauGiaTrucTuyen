package server.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import shared.model.auction.Auction;
import shared.model.auction.AutoBid;
import shared.model.auction.BidTransaction;
import shared.model.item.Item;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import shared.model.user.*;
import server.repository.AuctionManager;
import server.repository.ItemManager;

/**
 * Lớp DatabaseManager chịu trách nhiệm quản lý toàn bộ các thao tác Đọc/Ghi/Cập nhật dữ liệu
 * giữa cơ sở dữ liệu MySQL (được lưu trữ trên dịch vụ đám mây Aiven Cloud) và bộ nhớ RAM của chương trình.
 *
 * Chiến lược quản lý dữ liệu:
 * 1. Khi Server khởi động: Gọi loadEverything() để nạp tất cả User, Item, và Auction lên RAM.
 *    - Việc này giúp tối ưu hiệu năng vì các thao tác truy xuất sau đó chỉ cần thực hiện trên RAM.
 *    - Toàn bộ kết nối đến Cloud DB được gom nhóm để giảm thiểu số lần bắt tay (handshake) và độ trễ mạng.
 * 2. Trong quá trình chạy: Bất kỳ thay đổi nào của User, Item hay Auction sẽ được lưu ngay xuống DB
 *    để đảm bảo tính toàn vẹn dữ liệu ngay cả khi hệ thống gặp sự cố (Crash, Restart).
 * 3. Đồng bộ Trạng thái đấu giá: Lịch sử đặt giá (Bid History) và cấu hình đấu giá tự động (Auto Bid)
 *    được lưu trữ/cập nhật bằng cơ chế xóa-và-chèn-lại qua JDBC Batch để giữ đồng bộ tuyệt đối với RAM.
 */
public class DatabaseManager {

    /**
     * HÀM TỔNG: Load toàn bộ dữ liệu từ Database lên RAM khi Server bắt đầu khởi động.
     * Tối ưu hóa: Sử dụng chung 1 Connection duy nhất cho cả 3 hàm load con (User -> Item -> Auction)
     * giúp giảm thiểu đáng kể số lượng handshake thiết lập kết nối SSL đến Aiven Cloud, tăng tốc khởi động hệ thống.
     */
    public static void loadEverything() {
        System.out.println(">>> ĐANG KHỞI TẠO HỆ THỐNG TỪ AIVEN CLOUD...");

        try (Connection conn = DBConnection.getConnection()) {
            // Bước 0: Tự động khởi tạo các bảng phụ trợ (bid_transactions, auto_bids) nếu chưa tồn tại trong DB
            initializeTables(conn);

            // Bước 1: Nạp toàn bộ danh sách User từ DB và gán cho UserManager trên RAM
            List<User> allUsers = loadAllUsers(conn);
            UserManager.getInstance().setUsers(allUsers);
            System.out.println("- Đã nạp " + allUsers.size() + " người dùng.");

            // Bước 2: Nạp toàn bộ danh sách Item từ DB và gán cho ItemManager trên RAM
            List<Item> allItems = loadAllItems(conn, allUsers);
            ItemManager.getInstance().setItems(allItems);
            System.out.println("- Đã nạp " + allItems.size() + " vật phẩm.");

            // Bước 3: Nạp toàn bộ danh sách Auction từ DB và gán cho AuctionManager trên RAM
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
    // PHẦN 1: QUẢN LÝ THÀNH VIÊN (USER)
    // ============================================================

    /**
     * Tải toàn bộ danh sách người dùng (Users) từ Database lên bộ nhớ RAM.
     * Sử dụng Connection được truyền vào từ loadEverything để tái sử dụng.
     * Hàm này thực hiện đọc cột 'role' để khởi tạo đúng loại đối tượng tương ứng: Admin, Bidder, hay Seller.
     *
     * @param conn Kết nối cơ sở dữ liệu hiện hành.
     * @return Danh sách đối tượng User được nạp đầy đủ thuộc tính số dư tài khoản.
     */
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
                // Nhóm 1: Người đặt giá (BIDDER)
                if ("BIDDER".equalsIgnoreCase(role)) {
                    double balance = rs.getDouble("balance");
                    double reservedBalance = rs.getDouble("reserved_balance"); // Số tiền đang bị khóa khi đang tham gia đấu giá

                    // Khởi tạo thực thể Bidder đầy đủ thuộc tính số dư khả dụng và số dư tạm khóa
                    u = new Bidder(id, username, password, fullName, balance, reservedBalance);
                }
                // Nhóm 2: Quản trị viên (ADMIN)
                else if ("ADMIN".equalsIgnoreCase(role)) {
                    u = new Admin(id, username, password, fullName);
                }
                // Nhóm 3: Người bán (SELLER)
                else {
                    double balance = rs.getDouble("balance");
                    Seller seller = new Seller(id, username, password, fullName);
                    // Dùng setBalanceLoaded thay vì setBalance thông thường để bỏ qua việc
                    // tự động gọi ngược lại DatabaseManager.updateUserState (tránh vòng lặp gọi DB vô tận khi load)
                    seller.setBalanceLoaded(balance);
                    u = seller;
                }

                list.add(u);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải người dùng: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lưu trữ thông tin một tài khoản người dùng mới đăng ký vào Database.
     * Sử dụng Statement.RETURN_GENERATED_KEYS để lấy ID tự động tăng sinh ra bởi MySQL
     * và đồng bộ ngược lại thuộc tính 'id' của đối tượng Java trên RAM.
     *
     * @param user Đối tượng người dùng mới cần lưu trữ.
     */
    public static void saveUser(User user) {
        String sql = "INSERT INTO users (username, password, fullName, role, balance, reserved_balance) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getFullName());

            // Chuyển đổi từ instance của Java Class sang String đại diện cho vai trò lưu trong MySQL
            String roleStr = "SELLER";
            if (user instanceof Bidder) roleStr = "BIDDER";
            else if (user instanceof Admin) roleStr = "ADMIN";

            pstmt.setString(4, roleStr);

            // Thiết lập giá trị số dư tài khoản tùy theo loại đối tượng người dùng cụ thể
            pstmt.setDouble(5, (user instanceof Bidder) ? ((Bidder) user).getBalance() : (user instanceof Seller) ? ((Seller) user).getBalance() : 0.0);
            pstmt.setDouble(6, (user instanceof Bidder) ? ((Bidder) user).getReservedBalance() : 0.0); // Chỉ có Bidder mới có cột reserved_balance

            pstmt.executeUpdate();

            // Lấy ID tự động sinh ra bởi MySQL (Auto-Increment) và cập nhật lại vào đối tượng Java
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

    /**
     * Cập nhật thông tin chi tiết (Mật khẩu, họ tên, số dư) của người dùng hiện tại trong Database.
     * Hàm này được kích hoạt tự động mỗi khi có biến động số dư hoặc chỉnh sửa hồ sơ trên RAM.
     *
     * @param user Đối tượng người dùng cần cập nhật.
     */
    public static void updateUserState(User user) {
        String sql = "UPDATE users SET password = ?, fullName = ?, balance = ?, reserved_balance = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getPassword());
            pstmt.setString(2, user.getFullName());

            // Cập nhật số dư đúng cho từng thực thể tương ứng
            pstmt.setDouble(3, (user instanceof Bidder) ? ((Bidder) user).getBalance() : (user instanceof Seller) ? ((Seller) user).getBalance() : 0.0);
            pstmt.setDouble(4, (user instanceof Bidder) ? ((Bidder) user).getReservedBalance() : 0.0); // Cập nhật số tiền đóng băng của Bidder
            pstmt.setString(5, user.getUsername());

            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật trạng thái người dùng: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // PHẦN 2: QUẢN LÝ SẢN PHẨM / VẬT PHẨM (ITEM)
    // ============================================================

    /**
     * Tải toàn bộ danh sách Vật phẩm (Items) từ Database lên RAM.
     * Sử dụng Java Stream API để tìm kiếm Seller (người bán) tương ứng từ danh sách Users đã tải trước đó,
     * thiết lập mối quan hệ liên kết thực thể (ORM thủ công).
     *
     * @param conn Kết nối cơ sở dữ liệu hiện hành.
     * @param allUsers Danh sách tất cả người dùng đã được tải lên RAM.
     * @return Danh sách sản phẩm Item.
     */
    public static List<Item> loadAllItems(Connection conn, List<User> allUsers) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT item_id, name, base_price, seller_username FROM items";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                String sellerName = rs.getString("seller_username");

                // Tìm kiếm an toàn đối tượng Seller trong danh sách users đã nạp trước đó
                User sellerUser = allUsers.stream()
                        .filter(u -> u.getUsername() != null && u.getUsername().trim().equalsIgnoreCase(sellerName.trim()))
                        .findFirst().orElse(null);

                // Ép kiểu an toàn (Safe Casting) để đảm bảo đối tượng sở hữu là một Seller thực thụ
                Seller validSeller = null;
                if (sellerUser instanceof Seller) {
                    validSeller = (Seller) sellerUser;
                } else {
                    System.err.println("[CẢNH BÁO] Không tìm thấy Seller hợp lệ cho Item ID: " + itemId);
                }

                // Khởi tạo ConcreteItem (lớp cụ thể kế thừa từ lớp trừu tượng Item)
                Item item = new ConcreteItem(itemId, rs.getString("name"), rs.getDouble("base_price"), validSeller);
                list.add(item);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải vật phẩm: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Xóa một vật phẩm khỏi Database dựa trên khóa chính item_id.
     *
     * @param itemId Mã số định danh của vật phẩm cần xóa.
     * @return true nếu xóa thành công ít nhất 1 dòng, ngược lại false.
     */
    public static boolean deleteItem(int itemId) {
        String sql = "DELETE FROM items WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            int rows = pstmt.executeUpdate();
            return rows > 0; // Trả về kết quả thực thi

        } catch (Exception e) {
            System.err.println("Lỗi khi xóa item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa một tài khoản người dùng khỏi Database dựa trên mã id.
     *
     * @param userId Mã số định danh của người dùng cần xóa.
     * @return true nếu xóa thành công ít nhất 1 dòng, ngược lại false.
     */
    public static boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (Exception e) {
            System.err.println("Lỗi khi xóa user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lưu thông tin một sản phẩm mới được người bán đăng tải lên hệ thống vào Database.
     * Đồng bộ ID tự động tăng sinh ra bởi MySQL vào đối tượng Java trên RAM.
     *
     * @param item Đối tượng vật phẩm mới cần lưu trữ.
     */
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

            // Đồng bộ khóa chính tự động tăng của sản phẩm ngược lại đối tượng RAM
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) item.setId(rs.getInt(1));
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu vật phẩm: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cập nhật thông tin chi tiết (tên, giá khởi điểm) của một sản phẩm hiện có trong Database.
     *
     * @param item Đối tượng vật phẩm cần cập nhật.
     */
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
    // PHẦN 3: QUẢN LÝ PHIÊN ĐẤU GIÁ (AUCTION)
    // ============================================================

    /**
     * Tải toàn bộ danh sách Phiên đấu giá (Auctions) từ Database lên RAM.
     * Sau khi khôi phục đối tượng phiên đấu giá, hàm tiếp tục:
     * 1. Nạp Lịch sử các lượt đấu giá (Bid History) từ bảng bid_transactions.
     * 2. Nạp Cấu hình đấu giá tự động (Auto Bid) từ bảng auto_bids.
     * 3. Tự động phục hồi bộ đếm giờ (scheduler timer task) nếu phiên đấu giá đang trong trạng thái RUNNING.
     *
     * @param conn Kết nối cơ sở dữ liệu hiện hành.
     * @param allItems Danh sách tất cả vật phẩm đã được tải lên RAM.
     * @param allUsers Danh sách tất cả người dùng đã được tải lên RAM.
     * @return Danh sách các phiên đấu giá đầy đủ trạng thái.
     */
    public static List<Auction> loadAllAuctions(Connection conn, List<Item> allItems, List<User> allUsers) {
        List<Auction> list = new ArrayList<>();
        // Đọc đầy đủ các cột trạng thái và các mốc thời gian đấu giá (lưu bằng Epoch Milliseconds)
        String sql = "SELECT auction_id, item_id, current_price, highest_bidder_username, status, start_time, end_time FROM auctions";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int auctionId = rs.getInt("auction_id");
                int itemId = rs.getInt("item_id");
                double currentPrice = rs.getDouble("current_price");
                String bidderName = rs.getString("highest_bidder_username");
                String statusString = rs.getString("status");
                long startTime = rs.getLong("start_time");
                long endTime = rs.getLong("end_time");

                // Đối chiếu và chuyển đổi chuỗi trạng thái từ DB sang Enum Auction.Status
                Auction.Status status;
                try {
                    status = Auction.Status.valueOf(statusString.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    System.err.println("[CẢNH BÁO] Bỏ qua phiên đấu giá " + auctionId + " vì trạng thái lỗi: " + statusString);
                    continue;
                }

                // Liên kết phiên đấu giá với vật phẩm (Item) tương ứng trên RAM
                Item item = allItems.stream()
                        .filter(i -> i.getId() == itemId)
                        .findFirst().orElse(null);

                // Liên kết phiên đấu giá với Bidder đang giữ giá cao nhất hiện tại
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
                    // 1. Khởi tạo an toàn qua Constructor 4 tham số (Mặc định ban đầu là OPEN)
                    Auction auction = new Auction(auctionId, item, item.getSeller(), item.getPrice());

                    // 2. Gán các thuộc tính động đọc từ DB lên RAM
                    auction.setCurrentPrice(currentPrice);
                    auction.setHighestBidder(validBidder);
                    auction.setStartTime(startTime);
                    auction.setEndTime(endTime);

                    // 3. ✅ SỬA TẠI ĐÂY: Gán trực tiếp trạng thái từ DB, bỏ qua bộ lọc transitionTo
                    auction.setStatusLoaded(status);

                    // 4. Phục hồi lịch sử đặt giá và các thiết lập AutoBid liên quan
                    loadBidHistory(conn, auction, allUsers);
                    loadAutoBids(conn, auction, allUsers);

                    // 5. Khôi phục bộ hẹn giờ chạy ngầm cho phiên đấu giá nếu phiên vẫn đang diễn ra (RUNNING)
                    if (status == Auction.Status.RUNNING) {
                        auction.resumeAfterRestart();
                        System.out.println(">>> Đã khôi phục bộ đếm ngược chạy ngầm cho phiên ID: " + auctionId);
                    }

                    list.add(auction);
                }
                else {
                    System.err.println("[CẢNH BÁO] Bỏ qua phiên đấu giá ID: " + auctionId + " do không tìm thấy vật phẩm tương ứng (Item ID: " + itemId + ").");
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải phiên đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Thêm mới hoặc Cập nhật trạng thái một Phiên đấu giá vào Database.
     * - Nếu ID phiên = 0: Thực hiện INSERT để tạo mới.
     * - Nếu ID phiên > 0: Thực hiện UPDATE các thông số động (giá hiện tại, người giữ giá, trạng thái, thời gian).
     * Sau đó, đồng bộ ngay lập tức danh sách lịch sử đặt giá và cấu hình AutoBid xuống DB.
     *
     * @param auction Đối tượng phiên đấu giá cần lưu trữ hoặc cập nhật.
     */
    public static void saveOrUpdateAuction(Auction auction) {
        String sql = (auction.getId() == 0)
                ? "INSERT INTO auctions (item_id, current_price, highest_bidder_username, status, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)"
                : "UPDATE auctions SET current_price = ?, highest_bidder_username = ?, status = ?, start_time = ?, end_time = ? WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Nhóm 1: Gán các tham số khi Thêm mới (INSERT)
            if (auction.getId() == 0) {
                pstmt.setInt(1, auction.getItem().getId());
                pstmt.setDouble(2, auction.getCurrentPrice());
                pstmt.setString(3, auction.getCurrentBidder() != null ? auction.getCurrentBidder().getUsername() : null);
                pstmt.setString(4, auction.getStatus().name());
                pstmt.setLong(5, auction.getStartTime());
                pstmt.setLong(6, auction.getEndTime());
            }
            // Nhóm 2: Gán các tham số khi Cập nhật (UPDATE)
            else {
                pstmt.setDouble(1, auction.getCurrentPrice());
                pstmt.setString(2, auction.getCurrentBidder() != null ? auction.getCurrentBidder().getUsername() : null);
                pstmt.setString(3, auction.getStatus().name());
                pstmt.setLong(4, auction.getStartTime());
                pstmt.setLong(5, auction.getEndTime());
                pstmt.setInt(6, auction.getId()); // Gán ID cho mệnh đề WHERE
            }

            pstmt.executeUpdate();

            // Nếu là phiên mới, gán ID tự động sinh ra vào đối tượng trên RAM
            if (auction.getId() == 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) auction.setId(rs.getInt(1));
            }

            // Đồng bộ dữ liệu phụ trợ liên kết (Lịch sử đấu giá & Đấu giá tự động) xuống DB dưới cùng kết nối
            saveBidHistory(conn, auction);
            saveAutoBids(conn, auction);

        } catch (Exception e) {
            System.err.println("Lỗi khi lưu/cập nhật phiên đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tự động khởi tạo cấu trúc bảng dữ liệu phụ trợ (bid_transactions và auto_bids)
     * nếu chưa tồn tại trong cơ sở dữ liệu MySQL của Aiven Cloud.
     * Hàm này được chạy tự động một lần khi hệ thống bắt đầu khởi động.
     *
     * @param conn Kết nối cơ sở dữ liệu dùng chung.
     */
    private static void initializeTables(Connection conn) {
        // Bảng chứa lịch sử chi tiết tất cả các lượt đặt giá của từng phiên đấu giá
        String createBidTransactionsTable = "CREATE TABLE IF NOT EXISTS bid_transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "auction_id INT," +
                "bidder_username VARCHAR(255)," +
                "amount DOUBLE," +
                "bid_time BIGINT" +
                ")";
        // Bảng chứa thông tin thiết lập AutoBid (đặt giá tự động) của từng Bidder cho mỗi phiên
        String createAutoBidsTable = "CREATE TABLE IF NOT EXISTS auto_bids (" +
                "auction_id INT," +
                "bidder_username VARCHAR(255)," +
                "max_bid DOUBLE," +
                "increment DOUBLE," +
                "timestamp BIGINT," +
                "PRIMARY KEY (auction_id, bidder_username)" +
                ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createBidTransactionsTable);
            stmt.execute(createAutoBidsTable);
            System.out.println(">>> Đã khởi tạo/kiểm tra các bảng lưu trữ bổ sung (bid_transactions, auto_bids) thành công.");
        } catch (SQLException e) {
            System.err.println("Lỗi khởi tạo bảng cơ sở dữ liệu: " + e.getMessage());
        }
    }

    /**
     * Tải và khôi phục lịch sử giao dịch đặt giá (Bid History) từ database cho một phiên đấu giá.
     * Ánh xạ các bản ghi thô từ SQL sang danh sách đối tượng Java `BidTransaction`.
     */
    private static void loadBidHistory(Connection conn, Auction auction, List<User> allUsers) {
        String sql = "SELECT bidder_username, amount, bid_time FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auction.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                List<BidTransaction> history = new ArrayList<>();
                while (rs.next()) {
                    String username = rs.getString("bidder_username");
                    double amount = rs.getDouble("amount");
                    long timeMillis = rs.getLong("bid_time");

                    // Tìm kiếm đối tượng Bidder sở hữu lượt đặt giá này trên RAM
                    Bidder bidder = (Bidder) allUsers.stream()
                            .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username))
                            .findFirst().orElse(null);

                    // Khôi phục đối tượng giao dịch lịch sử và chuyển đổi Epoch Millis về LocalDateTime cục bộ
                    if (bidder != null) {
                        LocalDateTime bidTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(timeMillis),
                                ZoneId.systemDefault()
                        );
                        history.add(new BidTransaction(bidder, amount, bidTime));

                        // Tự động khôi phục đăng ký Observer cho Bidder này
                        // Giúp Bidder nhận được thông báo real-time ngay khi login lại mà không cần đặt giá lại
                        auction.addObserver(bidder);
                    }
                }
                // Đồng bộ lịch sử giao dịch vào thuộc tính của phiên đấu giá trên RAM
                auction.setBidHistory(history);
            }
        } catch (Exception e) {
            System.err.println("Lỗi nạp lịch sử đặt giá cho phiên " + auction.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Tải và khôi phục các thiết lập Đấu giá tự động (Auto Bid) từ database cho một phiên đấu giá cụ thể.
     */
    private static void loadAutoBids(Connection conn, Auction auction, List<User> allUsers) {
        String sql = "SELECT bidder_username, max_bid, increment, timestamp FROM auto_bids WHERE auction_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auction.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                List<AutoBid> autoBidList = new ArrayList<>();
                while (rs.next()) {
                    String username = rs.getString("bidder_username");
                    double maxBid = rs.getDouble("max_bid");
                    double increment = rs.getDouble("increment");
                    long timestamp = rs.getLong("timestamp");

                    // Tìm kiếm đối tượng Bidder đã đăng ký thiết lập AutoBid này
                    Bidder bidder = (Bidder) allUsers.stream()
                            .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username))
                            .findFirst().orElse(null);

                    // Phục hồi đối tượng AutoBid
                    if (bidder != null) {
                        autoBidList.add(new AutoBid(bidder, maxBid, increment, timestamp));

                        // Tự động đăng ký lại Bidder này làm Observer cho phiên đấu giá
                        // Đảm bảo họ nhận được thông báo khi hệ thống tự động trả giá thay họ
                        auction.addObserver(bidder);
                    }
                }
                // Đồng bộ danh sách AutoBid vào phiên đấu giá trên RAM
                auction.setAutoBids(autoBidList);
            }
        } catch (Exception e) {
            System.err.println("Lỗi nạp tự động đặt giá cho phiên " + auction.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Lưu trữ toàn bộ lịch sử đấu giá hiện tại xuống database bằng JDBC Batch Update.
     * Quy trình:
     * 1. Xóa toàn bộ dữ liệu lịch sử cũ của phiên đấu giá này trong DB để tránh trùng lặp hoặc mâu thuẫn dữ liệu.
     * 2. Sử dụng PreparedStatement.addBatch() gom toàn bộ danh sách các lượt bid hiện có trên RAM và chèn lại trong một lượt gửi duy nhất (executeBatch).
     *
     * Cách tiếp cận Xóa-và-Chèn-Lại (Delete & Batch Insert) giúp đơn giản hóa logic, tránh việc so sánh
     * tìm phần tử khác biệt giữa RAM và DB (Delta-checking), đồng thời bảo toàn tốc độ ghi nhờ Batching.
     */
    private static void saveBidHistory(Connection conn, Auction auction) throws SQLException {
        // Bước 1: Xóa lịch sử cũ của phiên đấu giá hiện hành
        String deleteSql = "DELETE FROM bid_transactions WHERE auction_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, auction.getId());
            pstmt.executeUpdate();
        }

        // Bước 2: Chèn lại danh sách lịch sử mới từ RAM xuống DB bằng kỹ thuật Batching
        List<BidTransaction> history = auction.getBidHistory();
        if (history.isEmpty()) return;

        String insertSql = "INSERT INTO bid_transactions (auction_id, bidder_username, amount, bid_time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (BidTransaction tx : history) {
                pstmt.setInt(1, auction.getId());
                pstmt.setString(2, tx.getBidder().getUsername());
                pstmt.setDouble(3, tx.getBidAmount());
                pstmt.setLong(4, tx.getTimeMillis());
                pstmt.addBatch(); // Đưa câu lệnh INSERT này vào hàng đợi batch
            }
            pstmt.executeBatch(); // Thực thi đồng loạt toàn bộ câu lệnh INSERT trên Cloud DB
        }
    }

    /**
     * Lưu trữ toàn bộ các thiết lập AutoBid hiện tại xuống database bằng JDBC Batch Update.
     * Quy trình:
     * 1. Xóa tất cả cấu hình AutoBid cũ của phiên này trong DB.
     * 2. Đọc danh sách cấu hình AutoBid mới từ RAM, gom nhóm câu lệnh chèn vào Batch và ghi xuống DB.
     */
    private static void saveAutoBids(Connection conn, Auction auction) throws SQLException {
        // Bước 1: Xóa cấu hình AutoBid cũ của phiên
        String deleteSql = "DELETE FROM auto_bids WHERE auction_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, auction.getId());
            pstmt.executeUpdate();
        }

        // Bước 2: Ghi danh sách cấu hình AutoBid mới bằng Batching
        List<AutoBid> autoBidList = auction.getAutoBidList();
        if (autoBidList.isEmpty()) return;

        String insertSql = "INSERT INTO auto_bids (auction_id, bidder_username, max_bid, increment, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (AutoBid ab : autoBidList) {
                pstmt.setInt(1, auction.getId());
                pstmt.setString(2, ab.getBidder().getUsername());
                pstmt.setDouble(3, ab.getMaxBid());
                pstmt.setDouble(4, ab.getIncrement());
                pstmt.setLong(5, ab.getTimestamp());
                pstmt.addBatch(); // Đưa vào batch
            }
            pstmt.executeBatch(); // Thực thi hàng loạt
        }
    }
}

/**
 * ConcreteItem là lớp cụ thể hóa (concrete class) được thiết kế để kế thừa từ lớp trừu tượng Item.
 * Vì lớp Item gốc là một lớp trừu tượng (abstract), chúng ta cần ConcreteItem để có thể khởi tạo
 * các thực thể vật phẩm khi nạp dữ liệu thô từ cơ sở dữ liệu lên bộ nhớ RAM.
 */
class ConcreteItem extends Item {
    private final Seller seller; // Đối tượng người bán sở hữu vật phẩm này

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
        // Phương thức hiển thị (không cần thiết trong xử lý ngầm nên để trống)
    }
}
