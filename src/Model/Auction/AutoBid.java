package Model.Auction;

import Model.User.Bidder;



class AutoBid {
    private Bidder bidder;
    private double maxBid;
    private double increment;
    private long timestamp;

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