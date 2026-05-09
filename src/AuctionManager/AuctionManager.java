package AuctionManager;

import Auction.Auction;
import Item.*;
import User.Bidder;
import User.Seller;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {

    private static AuctionManager instance;
    private int count = 0;


    private final List<Auction> auctions = new ArrayList<>();


    private final ReentrantLock lock = new ReentrantLock();

    private AuctionManager(){}


    public static synchronized AuctionManager getInstance(){
        if (instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }


    public void newAuction(int itemId, Seller seller, double startPrice) {
        lock.lock();
        try {
            // lấy item từ ItemManager
            Item item = ItemManager.getInstance().getById(itemId);

            if (item == null) {
                throw new IllegalArgumentException("Item không tồn tại");
            }

            // tạo auction (id đã tự sinh bên trong)
            int id = count;
            count ++;
            Auction a = new Auction(id,item, seller, startPrice);

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
    public String getAuction() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder("LIST_AUCTION");

            for (Auction a : auctions) {
                sb.append(" ").append(a.getId());
            }

            return sb.toString();

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
    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions);
    }



    public boolean payAuction(int auctionId){
        Auction auction = getAuctionById(auctionId);
        if (auction == null) return false;

        auction.pay();
        return true;
    }
}