package Auction;
import Item.*;
import User.*;

public class Auction {
    private int id;
    Item bidItem;
    private Seller seller;

    public void setId(int id) {
        this.id = id;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public Seller getSeller() {
        return seller;
    }

    public int getId() {
        return id;
    }

    double currentPrice;
    Bidder currentBidder;

    public Auction(int id, Item bidItem, Seller seller, double currentPrice, Bidder currentBidder) {
        this.id = id;
        this.bidItem = bidItem;
        this.seller = seller;
        this.currentPrice = currentPrice;
        this.currentBidder = currentBidder;
    }
}
