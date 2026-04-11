package AuctionManager;
import java.util.ArrayList;

import Auction.BidTransaction;

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

}
