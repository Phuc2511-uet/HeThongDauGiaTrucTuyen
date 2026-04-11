package Auction;
import Item.*;
import User.*;

public class BidTransaction {
    private Item itemSold;
    private double finalPrice;
    private Bidder finalBidder;

    public BidTransaction(Item itemSelled, double finalPrice, Bidder finalBidder) {
        this.itemSold = itemSelled;
        this.finalPrice = finalPrice;
        this.finalBidder = finalBidder;
    }
}
