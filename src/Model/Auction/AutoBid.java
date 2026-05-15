package Model.Auction;

import Model.User.Bidder;

public class AutoBid {
    private final Bidder bidder;
    private double maxPrice;
    private long timestamp;

    public AutoBid(Bidder bidder, double maxPrice) {
        this.bidder = bidder;
        this.maxPrice = maxPrice;
        this.timestamp = System.currentTimeMillis();
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }
}