package Model.AuctionManager;

import Model.Auction.Auction;
import Model.Item.Item;
import Model.Item.ItemManager;
import Model.User.Bidder;
import Model.User.Seller;
import Controllers.Base.DatabaseManager; // Import DatabaseManager


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {

    private static AuctionManager instance;
    private int count = 0;


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
        // Cập nhật count để tránh trùng ID khi tải từ DB
        if (!auctions.isEmpty()) {
            this.count = auctions.stream().mapToInt(Auction::getId).max().orElse(0) + 1;
        }
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
            DatabaseManager.saveOrUpdateAuction(a); // Tự động lưu vào DB

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
            DatabaseManager.saveOrUpdateAuction(auction); // Tự động cập nhật vào DB
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
        DatabaseManager.saveOrUpdateAuction(auction); // Tự động cập nhật vào DB
        return true;
    }

    // Phương thức trả về danh sách ID|Status để hiển thị lên TableView của Client
    public String getAuctionListForClient() {
        lock.lock();
        try {
            if (auctions.isEmpty()) {
                return ""; // Hoặc trả về một thông báo trống
            }
            StringBuilder sb = new StringBuilder();
            for (Auction a : auctions) {
                // Định dạng: ID|Status (Status thay dấu cách bằng gạch dưới)
                String status = a.getStatus().name().replace(" ", "_");
                sb.append(a.getId()).append("|").append(status).append(" ");
            }
            return sb.toString().trim();
        } finally {
            lock.unlock();
        }
    }

    // Phương thức trả về chi tiết 1 Auction khi người dùng click vào xem chi tiết
    public String getAuctionDetailMessage(int id) {
        Auction a = getAuctionById(id);
        if (a == null) return "ERROR Auction_not_found";
        // Định dạng: AUCTION_DETAIL_SUCCESS <ID> <ItemName> <Price> <Seller> <Status>
        return String.format("AUCTION_DETAIL_SUCCESS %d %s %.2f %s %s",
                a.getId(),
                a.getItem().getName().replace(" ", "_"),
                a.getCurrentPrice(),
                a.getSeller().getFullName().replace(" ", "_"),
                a.getStatus().name().replace(" ", "_")
        );
    }
}