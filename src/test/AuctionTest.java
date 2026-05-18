package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import Model.Auction.Auction;
import Model.Item.Item;
import Model.User.Bidder;
import Model.User.Seller;

public class AuctionTest {

    private Seller seller;
    private Bidder bidder1;
    private Bidder bidder2;
    private Item item;
    private Auction auction;

    @BeforeEach
    public void setUp() {
        // Khởi tạo User theo đúng logic hệ thống của hai ông
        seller = new Seller(1, "seller1", "123", "Người Bán A");
        bidder1 = new Bidder(2, "bidder1", "123", "Người Đấu Giá 1", 2000.0); // Nâng ví lên $2000 để test bước nhảy
        bidder2 = new Bidder(3, "bidder2", "123", "Người Đấu Giá 2", 500.0);

        // Khởi tạo Item (gán constructor nhận 3 tham số của hai ông)
        item = new Item(1, "iPhone 15 Pro Max", 800.0) {
            @Override
            public Seller getSeller() {
                return seller;
            }
            @Override
            public void display() {
                System.out.println("Displaying item in test");
            }
        };

        // Dùng constructor phiên mới tạo (4 tham số) của hai ông cho sạch gọn
        // Constructor: id, bidItem, seller, startPrice
        auction = new Auction(1, item, seller, 800.0);
    }

    @Test
    public void testInitialAuctionState() {
        // Gọi chuẩn xác hàm getCurrentPrice() và getStatus() Enum từ code gốc
        assertEquals(800.0, auction.getCurrentPrice(), "Giá khởi điểm phải là 800.0");
        assertNull(auction.getCurrentBidder(), "Phiên mới tạo chưa có ai đặt giá");
        assertEquals(Auction.Status.OPEN, auction.getStatus(), "Trạng thái ban đầu phải là OPEN");
    }

    @Test
    public void testValidBidAction() {
        // Thử đặt giá hợp lệ lần đầu ($900 > $800)
        // Hàm placeBid tự động chuyển trạng thái từ OPEN sang RUNNING nên không cần setStatus bằng tay
        auction.placeBid(900.0, bidder1);

        assertEquals(900.0, auction.getCurrentPrice(), "Giá hiện tại phải nhảy lên 900.0");
        assertEquals(bidder1, auction.getCurrentBidder(), "Người ôm thầu cao nhất phải là bidder1");
        assertEquals(Auction.Status.RUNNING, auction.getStatus(), "Trạng thái phải tự động chuyển sang RUNNING");
    }

    @Test
    public void testBidLowerThanCurrentPrice() {
        // Lần đầu thầu lên $900 hợp lệ
        auction.placeBid(900.0, bidder1);

        // Lần hai: Gặp case "chuối" - bidder2 đặt giá $850 (thấp hơn giá hiện tại $900)
        // Code của hai ông sẽ ném ra ngoại lệ IllegalArgumentException ở dòng 173
        assertThrows(IllegalArgumentException.class, () -> {
            auction.placeBid(850.0, bidder2);
        }, "Hệ thống phải chặn đứng khi đặt giá thấp hơn giá hiện tại");
    }

    @Test
    public void testBidIncrementRule() {
        // Lần đầu thầu lên $900 hợp lệ (Phiên chuyển sang RUNNING)
        auction.placeBid(900.0, bidder1);

        // Lần hai: Gặp case "chuối" tiếp - Theo code của hai ông (dòng 177): "Min increment 100"
        // bidder2 đặt giá $950 (chỉ tăng $50 -> vi phạm luật tăng tối thiểu $100)
        assertThrows(IllegalArgumentException.class, () -> {
            auction.placeBid(950.0, bidder2);
        }, "Hệ thống phải chặn đứng khi bước nhảy nhỏ hơn 100");
    }
}