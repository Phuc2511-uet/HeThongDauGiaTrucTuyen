package shared.model.user;

import server.repository.AuctionManager;
import server.repository.UserManager;
import java.io.PrintWriter;

public class Admin extends User {

    // Biến out giúp Admin có thể nhận log hoặc thông báo từ Server gửi về ClientConnection của Admin
    private transient PrintWriter out;

    public Admin(int id, String username, String password, String fullName) {
        super(id, username, password, fullName);
    }

    public void setConnection(PrintWriter out) {
        this.out = out;
    }

    // ===== CÁC QUYỀN HẠN CỦA ADMIN (Gợi ý mở rộng) =====

    /**
     * Admin có quyền hủy bỏ một phiên đấu giá bất kỳ nếu phát hiện gian lận
     */
    public boolean forceCancelAuction(int auctionId) {
        System.out.println("[ADMIN ACTION] Đang hủy phiên đấu giá lỗi ID: " + auctionId);
        // Sau này gọi sang AuctionManager để xử lý:
        // AuctionManager.getInstance().getAuctionById(auctionId).cancel();
        return true;
    }


}