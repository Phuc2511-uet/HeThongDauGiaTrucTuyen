package Auction;

import Item.Item;
import User.Bidder;

import java.time.LocalDateTime;
import java.util.UUID;

public class BidTransaction {


    private final Item item;
    private final Bidder bidder;
    private final double bidAmount;
    private final LocalDateTime bidTime;

    public BidTransaction(Item item, Bidder bidder, double bidAmount) {
        if (item == null || bidder == null) {
            throw new IllegalArgumentException("Item and Bidder must not be null");
        }
        if (bidAmount <= 0) {
            throw new IllegalArgumentException("Bid amount must be > 0");
        }


        this.item = item;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }



    public Item getItem() {
        return item;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    @Override
    public String toString() {
        return "BidTransaction{" +

                ", item=" + item +
                ", bidder=" + bidder +
                ", amount=" + bidAmount +
                ", time=" + bidTime +
                '}';
    }
}