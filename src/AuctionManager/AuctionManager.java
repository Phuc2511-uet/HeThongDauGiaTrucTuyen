package AuctionManager;

import Auction.Auction;
import Item.Item;
import User.Bidder;
import User.Seller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {

    private static AuctionManager instance;


    private final List<Auction> auctions = new ArrayList<>();


    private final ReentrantLock lock = new ReentrantLock();

    private AuctionManager(){}


    public static synchronized AuctionManager getInstance(){
        if (instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }


    public void newAuction(int id, Item bidItem, Seller seller, double startPrice){
        lock.lock();
        try {
            Auction a = new Auction(id, bidItem, seller, startPrice);
            auctions.add(a);
        } finally {
            lock.unlock();
        }
    }


    public Auction getAuctionById(int id){
        lock.lock();
        try {
            for (Auction a : auctions){

                if (a.getId() == id){
                    return a;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    // ===== LẤY DANH SÁCH =====
    public List<Auction> getAuctions(){
        lock.lock();
        try {
            return new ArrayList<>(auctions); // tránh lộ reference
        } finally {
            lock.unlock();
        }
    }

    // =====  THÊM BID MỚI  =====
    public boolean placeBid(int auctionId, Bidder bidder, double price){
        Auction auction;


        lock.lock();
        try {
            auction = getAuctionById(auctionId);
        } finally {
            lock.unlock();
        }

        if (auction == null){
            return false;
        }


        try {
            auction.placeBid(price, bidder);
            return true;
        } catch (Exception e){
            System.out.println("Bid failed: " + e.getMessage());
            return false;
        }
    }



    public boolean payAuction(int auctionId){
        Auction auction = getAuctionById(auctionId);
        if (auction == null) return false;

        auction.pay();
        return true;
    }
}