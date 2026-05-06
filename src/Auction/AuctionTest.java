package Auction;
import Auction.Auction;
import Item.*;
import User.Bidder;
import User.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {
    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;
    private Item item;
    private Seller seller;

    @BeforeEach
    void setUp() {
        // Khởi tạo dữ liệu giả lập trước mỗi test
        item = new Art("I01", "Bức tranh", 1000.0);
        seller = new Seller("S01", "sellera", "123","Nguyen Van A");
        bidder1 = new Bidder("B01", "Tran Van B", "bidderb", "123", 5000.0);
        bidder2 = new Bidder("B02", "Le Thi C", "bidderc", "123", 5000.0);

        // Khởi tạo phiên đấu giá với giá khởi điểm 1000
        auction = new Auction(1, item, seller, 1000.0);
    }

    @Test
    @DisplayName("Test đặt giá lần đầu thành công và chuyển trạng thái sang RUNNING")
    void testFirstBidSuccess() {
        auction.placeBid(1500.0, bidder1);
        assertEquals(1500.0, auction.getCurrentPrice(), "Giá hiện tại phải được cập nhật lên 1500");
        assertTrue(auction.getRemainingTime() > 0, "Phiên đấu giá phải đang chạy và có thời gian còn lại");
    }

    @Test
    @DisplayName("Test đặt giá thấp hơn giá khởi điểm phải báo lỗi")
    void testInvalidBidLowPrice() {
        // Mong đợi ném ra IllegalArgumentException khi đặt 900 (thấp hơn 1000)
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            auction.placeBid(900.0, bidder1);
        });

        String expectedMessage = "Must be higher than start price";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    @DisplayName("Test bước giá tối thiểu (phải cao hơn ít nhất 100)")
    void testMinIncrement() {
        // Đặt giá đầu tiên thành công
        auction.placeBid(1500.0, bidder1);

        // Đặt giá tiếp theo chỉ hơn 50 đơn vị (1550) -> Phải lỗi
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            auction.placeBid(1550.0, bidder2);
        });

        assertTrue(exception.getMessage().contains("Min increment 100"));
    }

    @Test
    @DisplayName("Test tự động gia hạn thời gian khi có người đặt giá")
    void testExtendAuction() {
        auction.placeBid(1500.0, bidder1);
        long timeBefore = auction.getRemainingTime();

        // Đặt giá mới để kích hoạt extendAuction()
        auction.placeBid(2000.0, bidder2);
        long timeAfter = auction.getRemainingTime();

        // Thời gian sau phải lớn hơn thời gian trước (do được cộng thêm 1 phút)
        assertTrue(timeAfter > timeBefore, "Thời gian kết thúc phải được gia hạn");
    }
}