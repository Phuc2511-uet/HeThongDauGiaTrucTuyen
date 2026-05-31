package test;

import shared.exception.AuthenticationException;
import shared.exception.InsufficientBalanceException;
import shared.model.auction.Auction;
import shared.model.auction.BidTransaction;
import shared.model.item.Art;
import shared.model.item.Item;
import shared.model.user.Bidder;
import shared.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    private Seller seller;
    private Bidder bidder;
    private Bidder secondBidder;
    private Item item;
    private Auction auction;

    @BeforeEach
    void setUp() {
        seller = new Seller(1, "seller01", "pass", "Test Seller");

        bidder = new Bidder(2, "bidder01", "pass", "Test Bidder", 5_000.0, 500.0);
        secondBidder = new Bidder(3, "bidder02", "pass", "Second Bidder", 7_000.0, 0.0);

        item = new Art( "Painting", 1_000.0, seller);
        auction = new Auction(20, item, seller, item.getPrice());
    }

    @Test
    void newAuctionStartsOpenWithBasePrice() {
        assertEquals(20, auction.getId());
        assertSame(item, auction.getItem());
        assertSame(seller, auction.getSeller());
        assertEquals(Auction.Status.OPEN, auction.getStatus());
        assertEquals(1_000.0, auction.getCurrentPrice());
        assertNull(auction.getCurrentBidder());
        assertEquals(0, auction.getStartTime());
        assertEquals(0, auction.getEndTime());
    }

    @Test
    void loadedAuctionKeepsDatabaseState() {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 60_000;

        Auction loadedAuction = new Auction(
                30,
                item,
                seller,
                item.getPrice(),
                1_500.0,
                bidder,
                Auction.Status.RUNNING,
                startTime,
                endTime
        );

        assertEquals(Auction.Status.RUNNING, loadedAuction.getStatus());
        assertEquals(1_500.0, loadedAuction.getCurrentPrice());
        assertSame(bidder, loadedAuction.getCurrentBidder());
        assertEquals(startTime, loadedAuction.getStartTime());
        assertEquals(endTime, loadedAuction.getEndTime());
        assertTrue(loadedAuction.getRemainingTime() > 0);
    }

    @Test
    void bidTransactionRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(bidder, 0));
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(bidder, -100));
    }

    @Test
    void bidTransactionStoresBidderAmountAndTime() {
        BidTransaction transaction = new BidTransaction(bidder, 1_200.0);

        assertSame(bidder, transaction.getBidder());
        assertEquals(1_200.0, transaction.getBidAmount());
        assertNotNull(transaction.getBidTime());
        assertTrue(transaction.getTimeMillis() > 0);
    }

    @Test
    void bidHistoryGetterReturnsDefensiveCopy() {
        List<BidTransaction> history = auction.getBidHistory();
        history.add(new BidTransaction(bidder, 1_200.0));

        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void bidderAvailableBalanceSubtractsReservedBalance() {
        assertEquals(4_500.0, bidder.getAvailableBalance());
    }

    @Test
    void bidderBalanceCheckRejectsAmountGreaterThanAvailableBalance() {
        assertDoesNotThrow(() -> bidder.checkBalance(4_500.0));

        InsufficientBalanceException exception = assertThrows(
                InsufficientBalanceException.class,
                () -> bidder.checkBalance(4_501.0)
        );
        assertNotNull(exception.getMessage());
    }

    @Test
    void userLoginAcceptsCorrectPasswordAndRejectsWrongPassword() {
        assertDoesNotThrow(() -> bidder.login("pass"));
        assertThrows(AuthenticationException.class, () -> bidder.login("wrong-pass"));
    }

    @Test
    void registerAutoBidRequiresMaxBidGreaterThanCurrentPrice() {
        assertThrows(IllegalArgumentException.class, () -> auction.registerAutoBid(bidder, 1_000.0, 100.0));
        assertThrows(IllegalArgumentException.class, () -> auction.registerAutoBid(bidder, 900.0, 100.0));

        assertDoesNotThrow(() -> auction.registerAutoBid(bidder, 1_500.0, 100.0));
    }

    @Test
    void registerAutoBidRejectsDuplicateBidder() {
        auction.registerAutoBid(secondBidder, 2_000.0, 100.0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> auction.registerAutoBid(secondBidder, 2_500.0, 200.0)
        );
        assertNotNull(exception.getMessage());
    }
}
