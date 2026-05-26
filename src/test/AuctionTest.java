package test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AuctionTest {

    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        // Khởi tạo kết nối cơ sở dữ liệu dựa trên cấu hình dự án
        connection = Controllers.Base.DBConnection.getConnection();

        // Vô hiệu hóa Auto-Commit để kích hoạt cơ chế Transaction Rollback
        connection.setAutoCommit(false);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null) {
            // Thực hiện hủy bỏ toàn bộ các thao tác ghi dữ liệu phát sinh trong bài test
            connection.rollback();

            // Đóng kết nối để giải phóng tài nguyên hệ thống
            connection.close();
        }
    }


    // 1. KIỂM THỬ PHÂN HỆ QUẢN LÝ NGƯỜI DÙNG (USERS TABLE)


    /**
     * Chức năng kiểm thử: Đăng ký và khởi tạo tài khoản người dùng mới.
     * Kiểm tra khả năng chèn bản ghi hợp lệ vào bảng 'users'.
     */
    @Test
    public void testUserRegistrationSuccess() throws SQLException {
        String sql = "INSERT INTO users (username, password, balance) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "user123");
            pstmt.setString(2, "123");
            pstmt.setDouble(3, 2000.0);

            int rowsAffected = pstmt.executeUpdate();
            // Xác nhận số dòng bị tác động trong cơ sở dữ liệu phải bằng 1
            assertEquals(1, rowsAffected);
        }
    }

    /**
     * Chức năng kiểm thử: Cập nhật số dư tài khoản người dùng.
     * Xác minh logic cộng tiền vào ví khi người dùng thực hiện nạp tiền.
     */
    @Test
    public void testUpdateUserBalanceSuccess() throws SQLException {
        String insertSql = "INSERT INTO users (username, password, balance) VALUES (?, ?, ?)";
        String updateSql = "UPDATE users SET balance = balance + ? WHERE username = ?";

        try (PreparedStatement insStmt = connection.prepareStatement(insertSql);
             PreparedStatement updStmt = connection.prepareStatement(updateSql)) {

            // Tạo dữ liệu người dùng mẫu phục vụ kiểm thử
            insStmt.setString(1, "user123");
            insStmt.setString(2, "123");
            insStmt.setDouble(3, 500.0);
            insStmt.executeUpdate();

            // Thực thi cập nhật tăng số dư tài khoản
            updStmt.setDouble(1, 150.5);
            updStmt.setString(2, "user123");
            int rowsUpdated = updStmt.executeUpdate();

            // Xác nhận việc cập nhật trạng thái số dư thành công
            assertEquals(1, rowsUpdated);
        }
    }

    /**
     * Chức năng kiểm thử: Truy vấn người dùng bằng định danh không tồn tại.
     * Xác minh hệ thống xử lý an toàn, trả về tập kết quả rỗng thay vì gây lỗi hệ thống.
     */
    @Test
    public void testFindUserWithInvalidUsername() throws SQLException {
        String sql = "SELECT * FROM users WHERE username = 'non_existent_user_123'";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            // Kết quả trả về bắt buộc phải là rỗng (false)
            assertFalse(rs.next());
        }
    }
    // 2. KIỂM THỬ PHÂN HỆ QUẢN LÝ VẬT PHẨM ĐẤU GIÁ (ITEMS TABLE)

    /**
     * Chức năng kiểm thử: Đăng ký thông tin vật phẩm mới lên hệ thống.
     * Kiểm tra cấu trúc ràng buộc dữ liệu của bảng 'items'.
     */
    @Test
    public void testCreateNewItemSuccess() throws SQLException {
        String sql = "INSERT INTO items (name, base_price, seller_username, category, description) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "Laptop");
            pstmt.setDouble(2, 750.0);
            pstmt.setString(3, "sellerA");
            pstmt.setString(4, "Electronics");
            pstmt.setString(5, "Descrip");

            int rowsAffected = pstmt.executeUpdate();
            // Xác nhận bản ghi vật phẩm được khởi tạo thành công
            assertEquals(1, rowsAffected);
        }
    }

    /**
     * Chức năng kiểm thử: Truy vấn và lọc dữ liệu vật phẩm theo danh mục sản phẩm.
     * Xác minh khả năng đọc dữ liệu tạm thời trong cùng một Transaction.
     */
    @Test
    public void testFilterItemsByCategory() throws SQLException {
        String insertSql = "INSERT INTO items (name, base_price, seller_username, category, description) VALUES (?, ?, ?, ?, ?)";
        String selectSql = "SELECT * FROM items WHERE category = ?";

        try (PreparedStatement insStmt = connection.prepareStatement(insertSql);
             PreparedStatement selStmt = connection.prepareStatement(selectSql)) {

            // Khởi tạo một sản phẩm thuộc danh mục 'Art'
            insStmt.setString(1, "Tranh");
            insStmt.setDouble(2, 300.0);
            insStmt.setString(3, "user123");
            insStmt.setString(4, "Art");
            insStmt.setString(5, "descrip");
            insStmt.executeUpdate();

            // Tìm kiếm các sản phẩm có danh mục tương ứng
            selStmt.setString(1, "Art");
            try (ResultSet rs = selStmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("Tranh", rs.getString("name"));
            }
        }
    }

    /**
     * Chức năng kiểm thử: Xóa bản ghi vật phẩm khỏi hệ thống.
     * Đảm bảo câu lệnh DELETE thực thi đúng điều kiện và phạm vi ảnh hưởng.
     */
    @Test
    public void testDeleteItemSuccess() throws SQLException {
        String insertSql = "INSERT INTO items (name, base_price, seller_username, category, description) VALUES (?, ?, ?, ?, ?)";
        String deleteSql = "DELETE FROM items WHERE name = ? AND seller_username = ?";

        try (PreparedStatement insStmt = connection.prepareStatement(insertSql);
             PreparedStatement delStmt = connection.prepareStatement(deleteSql)) {

            // Tạo vật phẩm trung gian để chuẩn bị xóa
            insStmt.setString(1, "phone");
            insStmt.setDouble(2, 10.0);
            insStmt.setString(3, "user123");
            insStmt.setString(4, "Test");
            insStmt.setString(5, "descrip");
            insStmt.executeUpdate();

            // Thực thi lệnh xóa dựa trên tên sản phẩm và người bán
            delStmt.setString(1, "phone");
            delStmt.setString(2, "user123");
            int rowsDeleted = delStmt.executeUpdate();

            // Xác nhận hệ thống đã xóa chính xác 1 bản ghi
            assertEquals(1, rowsDeleted);
        }
    }


    // 3. KIỂM THỬ PHÂN HỆ PHÒNG ĐẤU GIÁ (AUCTIONS TABLE)


    /**
     * Chức năng kiểm thử: Khởi tạo phòng đấu giá mới liên kết với vật phẩm hợp lệ.
     * Giải quyết ràng buộc khóa ngoại bằng cách lấy ID tự động phát sinh từ bảng 'items'.
     */
    @Test
    public void testCreateAuctionRoomSuccess() throws SQLException {
        String itemSql = "INSERT INTO items (name, base_price, category, description) VALUES (?, ?, ?, ?)";
        int generatedItemId = -1;

        // Bước 1: Khởi tạo dữ liệu tầng cha để thu thập khóa chính (item_id)
        try (PreparedStatement itemPstmt = connection.prepareStatement(itemSql, Statement.RETURN_GENERATED_KEYS)) {
            itemPstmt.setString(1, "test item");
            itemPstmt.setDouble(2, 500.0);
            itemPstmt.setString(3, "Test");
            itemPstmt.setString(4, "descrip");
            itemPstmt.executeUpdate();

            try (ResultSet rs = itemPstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedItemId = rs.getInt(1);
                }
            }
        }

        // Bước 2: Liên kết mã vật phẩm hợp lệ vào bản ghi phòng đấu giá mới
        String sql = "INSERT INTO auctions (item_id, current_price, highest_bidder_username, status, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, generatedItemId);
            pstmt.setDouble(2, 800.0);
            pstmt.setString(3, null);
            pstmt.setString(4, "ACTIVE");
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.setLong(6, System.currentTimeMillis() + 7200000);

            assertEquals(1, pstmt.executeUpdate());
        }
    }

    /**
     * Chức năng kiểm thử: Cập nhật thông tin người trả giá cao nhất.
     * Mô phỏng hành vi khi có phiên đặt giá mới hợp lệ làm thay đổi trạng thái phòng đấu giá.
     */
    @Test
    public void testUpdateAuctionHighestBidder() throws SQLException {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_username = ? WHERE status = 'ACTIVE'";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, 1050.0);
            pstmt.setString(2, "user123");

            int rowsAffected = pstmt.executeUpdate();
            // Đảm bảo cú pháp thực thi thành công, kết quả trả về lớn hơn hoặc bằng 0
            assertTrue(rowsAffected >= 0);
        }
    }

    /**
     * Chức năng kiểm thử: Truy vấn danh sách các phiên đấu giá đang trong trạng thái kích hoạt.
     * Đảm bảo logic đọc trạng thái không xảy ra ngoại lệ cú pháp.
     */
    @Test
    public void testQueryActiveAuctions() throws SQLException {
        String sql = "SELECT * FROM auctions WHERE status = 'ACTIVE'";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            // Xác nhận đối tượng ResultSet được khởi tạo thành công
            assertNotNull(rs);
        }
    }


    // 4. KIỂM THỬ LUỒNG NGHIỆP VỤ LIÊN THÔNG (INTEGRATION FLOW)

    /**
     * Chức năng kiểm thử: Giả lập chu trình nghiệp vụ khép kín.
     * Quy trình: Khởi tạo tài khoản -> Đăng bán vật phẩm -> Mở phòng đấu giá công khai.
     */
    @Test
    public void testComplexIntegrationFlow() throws SQLException {
        String userSql = "INSERT INTO users (username, password, balance) VALUES ('flow_user', 'pass', 5000.0)";
        String itemSql = "INSERT INTO items (name, base_price, seller_username, category, description) VALUES ('iPhone 15 Pro', 999.0, 'flow_user', 'Mobile', 'Thiết bị di động')";
        String auctionSql = "INSERT INTO auctions (item_id, current_price, status, start_time, end_time) VALUES (?, 999.0, 'PENDING', 0, 0)";

        try (PreparedStatement userPstmt = connection.prepareStatement(userSql);
             PreparedStatement itemPstmt = connection.prepareStatement(itemSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement auctionPstmt = connection.prepareStatement(auctionSql)) {

            // Thực thi chèn dữ liệu tầng người dùng và vật phẩm
            int userResult = userPstmt.executeUpdate();
            int itemResult = itemPstmt.executeUpdate();

            // Trích xuất mã vật phẩm vừa tạo từ cơ sở dữ liệu
            int generatedItemId = -1;
            try (ResultSet rs = itemPstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedItemId = rs.getInt(1);
                }
            }

            // Ràng buộc mã vật phẩm động vào câu lệnh khởi tạo phòng đấu giá
            auctionPstmt.setInt(1, generatedItemId);
            int auctionResult = auctionPstmt.executeUpdate();

            // Xác nhận cả 3 bước trong chuỗi liên thông đều thực thi thành công
            assertEquals(1, userResult);
            assertEquals(1, itemResult);
            assertEquals(1, auctionResult);
        }
    }
}