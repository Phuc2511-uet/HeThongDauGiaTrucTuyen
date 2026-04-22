package AuctionManager;
import java.util.ArrayList;

import Auction.Auction;
import Auction.BidTransaction;
import Item.Item;
import User.Bidder;
import User.Seller;


public class AuctionManager {
    private ArrayList<BidTransaction> history = new ArrayList<>();
    private static AuctionManager Manager;


    public ArrayList<BidTransaction> getHistory() {
        return history;
    }
    public void addBidHistory(BidTransaction n){
        history.add(n);
    }


    private AuctionManager(){}
    public static AuctionManager getInstance(){
        if (Manager == null){
            Manager = new AuctionManager();
            return Manager;
        }else{

            return Manager;
        }
    }
    public void placeBid(int id, Item bidItem, Seller seller, double currentPrice){
        Auction a = new Auction(id, bidItem, seller,currentPrice);

    }

}
