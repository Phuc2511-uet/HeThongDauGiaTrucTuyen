package Model.Auction;

import Model.User.Bidder;

public class AutoBid {
    private final Bidder bidder;
    private final double maxBid;
    private final double increment;
    private final long timestamp; // để ưu tiên

    public AutoBid(Bidder bidder, double maxBid, double increment) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
        this.timestamp = System.currentTimeMillis();
    }

    public Bidder getBidder() { return bidder; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
    public long getTimestamp() { return timestamp; }
}