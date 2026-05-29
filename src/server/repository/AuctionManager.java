package server.repository;

import shared.exception.AuctionClosedException;
import shared.exception.InvalidBidException;
import shared.model.auction.Auction;
import shared.model.item.Item;
import server.repository.ItemManager;
import shared.model.user.Bidder;
import shared.model.user.Seller;
import server.repository.DatabaseManager; // Import DatabaseManager


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {

    private static AuctionManager instance;
    private List<Auction> auctions = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private AuctionManager(){}

    public static synchronized AuctionManager getInstance(){
        if (instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }

    public void setAuctions(List<Auction> auctions) {
        this.auctions.addAll(auctions);
    }


    public void newAuction(int itemId, Seller seller, double startPrice) {
        lock.lock();
        try {
            // lấy item từ ItemManager
            Item item = ItemManager.getInstance().getById(itemId);

            if (item == null) {
                throw new IllegalArgumentException("Item không tồn tại");
            }

            // tạo auction (db đã tự sinh id)
            Auction a = new Auction(0,item, seller, startPrice);

            auctions.add(a);
            DatabaseManager.saveOrUpdateAuction(a); // Tự động lưu vào DB
        } finally {
            lock.unlock();
        }
    }
    public void restoreAuctions() {

        long now = System.currentTimeMillis();

        for (Auction a : auctions) {

            if (a.getStatus() == Auction.Status.RUNNING) {

                if (a.getEndTime() <= now) {
                    // ✅ quá hạn → finish ngay
                    a.forceFinish();
                } else {

                    a.resumeAfterRestart();
                }
            }
            System.out.println("Auction " + a.getId() +
                    " | endTime=" + a.getEndTime() +
                    " | now=" + now +
                    " | status=" + a.getStatus());
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
            List<Auction> auctions = getAllAuctions();

            for (Auction a : auctions) {
                // a.getStatus() là Enum, .name() sẽ trả về "OPEN", "RUNNING",...
                String statusStr = a.getStatus().name();

                sb.append(" ")
                        .append(a.getId())
                        .append("|")
                        .append(statusStr);
            }
            return sb.toString();
            // Kết quả gửi đi: "LIST_AUCTION 1|OPEN 2|RUNNING"
        } finally {
            lock.unlock();
        }
    }

    // =====  THÊM BID MỚI  =====
    public String placeBid(int auctionId, Bidder bidder, double price) throws AuctionClosedException, InvalidBidException {
        Auction auction;

        lock.lock();
        try {
            auction = getAuctionById(auctionId);
        } finally {
            lock.unlock();
        }

        if (auction == null){
            throw new InvalidBidException("Auction_không_tồn_tại");
        }

        // Gọi logic đặt giá bên trong Auction (Tại đây trạng thái sẽ đổi sang RUNNING và sinh ra endTime)
        auction.placeBid(price, bidder);

        // Lưu hoặc cập nhật phiên đấu giá mới vào Database MySQL
        DatabaseManager.saveOrUpdateAuction(auction);

        return this.getAuctionDetailMessage(auctionId);
    }

    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions);
    }


    public boolean payAuction(int auctionId) {

        Auction auction = getAuctionById(auctionId);

        if (auction == null) return false;

        boolean success = auction.pay();

        if (success) {

            //  XÓA ITEM TẠI ĐÂY
            ItemManager.getInstance().remove(auction.getItem().getId());

            // lưu DB
            DatabaseManager.saveOrUpdateAuction(auction);
        }

        return success;
    }


    // Phương thức trả về chi tiết 1 Auction khi người dùng click vào xem chi tiết
    public String getAuctionDetailMessage(int id) {
        Auction a = getAuctionById(id);
        if (a == null) return "ERROR Auction_not_found";

        // Gọi trực tiếp hàm định dạng chuỗi đã có sẵn trong Auction
        return "AUCTION_DETAIL_SUCCESS " + a.toNetworkString();
    }
    public Auction getAuctionByItemId(int itemId) {

        for (Auction a : auctions) {
            if (a.getItem().getId() == itemId) {
                return a;
            }
        }

        return null;
    }
}