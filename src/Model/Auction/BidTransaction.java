package Model.Auction;

import Model.Item.Item;
import Model.User.Bidder;

import java.time.LocalDateTime;

public class BidTransaction {



    private final Bidder bidder;
    private final double bidAmount;
    private final LocalDateTime bidTime;

    public BidTransaction(Bidder bidder, double bidAmount) {

        if (bidAmount <= 0) {
            throw new IllegalArgumentException("Bid amount must be > 0");
        }



        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }
    public long getTimeMillis() {
        return bidTime.atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
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


                ", bidder=" + bidder +
                ", amount=" + bidAmount +
                ", time=" + bidTime +
                '}';
    }
}