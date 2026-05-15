package Model.Auction;

import Controllers.Exceptions.AuctionClosedException;
import Controllers.Exceptions.InvalidBidException;
import Model.Item.Item;
import Model.User.Bidder;
import Model.User.Seller;
import Model.Observer.Observer;
import Controllers.Base.DatabaseManager; // Import DatabaseManager

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Auction {

    public enum Status {
        OPEN,
        RUNNING,
        FINISH,
        PAID,
        CANCELED
    }

    private Status currentStatus;
    // ===== AUTO BID =====
    private final java.util.Map<Integer, AutoBid> autoBidMap = new java.util.HashMap<>();

    private final java.util.PriorityQueue<AutoBid> autoBidQueue =
            new java.util.PriorityQueue<>((a, b) -> {
                int cmp = Double.compare(b.getMaxPrice(), a.getMaxPrice());
                if (cmp != 0) return cmp;
                return Long.compare(a.getTimestamp(), b.getTimestamp());
            });

    private static final double MIN_INCREMENT = 100;

    private List<Observer> observers = new ArrayList<>();
    private int id;
    private Item bidItem;
    private Seller seller;
    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private double currentPrice;
    private Bidder currentBidder;


    private final ReentrantLock lock = new ReentrantLock();

    // ===== CONSTRUCTOR cho Auction mới =====
    public Auction(int id, Item bidItem, Seller seller, double startPrice) {
        this.id = id;
        this.bidItem = bidItem;
        this.seller = seller;
        this.currentPrice = startPrice;
        this.currentStatus = Status.OPEN;
    }

    // ===== CONSTRUCTOR để tải từ Database =====
    public Auction(int id, Item bidItem, Seller seller, double startPrice, double currentPrice, Bidder currentBidder, Status status) {
        this.id = id;
        this.bidItem = bidItem;
        this.seller = seller;
        this.currentPrice = currentPrice; // currentPrice từ DB
        this.currentBidder = currentBidder; // currentBidder từ DB
        this.currentStatus = status; // status từ DB
        // startPrice có thể được lấy từ item.getPrice() hoặc lưu riêng nếu cần
    }


    public void setId(int id) {
        this.id = id;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setHighestBidder(Bidder bidder) {
        this.currentBidder = bidder;
    }

    // ===== GETTER CẦN THIẾT (QUAN TRỌNG) =====
    public int getId() {
        return id;
    }

    public Item getItem() {
        return bidItem;
    }

    public Seller getSeller() {
        return seller;
    }

    public Status getStatus() {
        return currentStatus;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public Bidder getCurrentBidder() {
        return currentBidder;
    }

    // 👉 helper cho server (rất nên có)
    public String toNetworkString() {
        return id + " "
                + bidItem.getName().replace(" ", "_") + " "
                + currentPrice + " "
                + seller.getUsername() + " "
                + currentStatus;
    }

    // ===== OBSERVER =====
    public void addObserver(Observer observer) {
        lock.lock();
        try {
            observers.add(observer);
        } finally {
            lock.unlock();
        }
    }

    public void removeObserver(Observer observer) {
        lock.lock();
        try {
            observers.remove(observer);
        } finally {
            lock.unlock();
        }
    }

    public void notifyObservers(String message) {
        List<Observer> targets;
        lock.lock();
        try {
            targets = new ArrayList<>(this.observers);
        } finally {
            lock.unlock();
        }

        for (Observer obs : targets) {
            obs.update(message);
        }
    }

    // ===== TIME =====
    private long startTime;
    private long endTime;

    private static final long DURATION = 60 * 60 * 1000;
    private static final long EXTEND_TIME = 60 * 1000;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> finishTask;

    // ===== STATE MACHINE =====
    private boolean canTransitionTo(Status next) {
        switch (currentStatus) {
            case OPEN:
                return next == Status.RUNNING || next == Status.CANCELED;
            case RUNNING:
                return next == Status.FINISH || next == Status.CANCELED;
            case FINISH:
                return next == Status.PAID;
            default:
                return false;
        }
    }

    private void transitionTo(Status next) {
        if (!canTransitionTo(next)) {
            throw new IllegalStateException("Invalid transition");
        }
        currentStatus = next;
        // Không gọi saveOrUpdateAuction ở đây vì các phương thức gọi nó sẽ tự gọi save
    }

    private void startAuction() {
        long now = System.currentTimeMillis();

        startTime = now;
        endTime = now + DURATION;

        transitionTo(Status.RUNNING);

        scheduleFinish();
    }

    private void scheduleFinish() {
        long delay = Math.max(0, endTime - System.currentTimeMillis());

        finishTask = scheduler.schedule(() -> {
            lock.lock();
            try {
                if (currentStatus == Status.RUNNING) {
                    transitionTo(Status.FINISH);
                    System.out.println("Auction auto finished");
                    DatabaseManager.saveOrUpdateAuction(this); // Lưu khi phiên đấu giá tự động kết thúc
                }
            } finally {
                lock.unlock();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void extendAuction() {
        endTime += EXTEND_TIME;

        if (finishTask != null) {
            finishTask.cancel(false);
        }

        scheduleFinish();
    }

    // ===== BID =====
    public void placeBid(double newPrice, Bidder bidder)
            throws AuctionClosedException, InvalidBidException {

        String message = null;
        boolean shouldAddObserver = false;

        lock.lock();
        try {

            // ===== CHECK BALANCE (QUAN TRỌNG) =====
            try {
                bidder.checkBalance(newPrice);
            } catch (Exception e) {
                throw new InvalidBidException("Không_đủ_số_dư");
            }

            // ===== CASE 1: OPEN =====
            if (currentStatus == Status.OPEN) {

                if (newPrice <= currentPrice) {
                    throw new InvalidBidException("Giá_Không_hợp_lệ");
                }

                currentPrice = newPrice;
                currentBidder = bidder;

                bidHistory.add(new BidTransaction(bidItem, bidder, newPrice));

                startAuction();
                handleAutoBid();

                if (!observers.contains(bidder)) {
                    shouldAddObserver = true;
                }

                return;
            }

            // ===== CASE 2: KHÔNG RUNNING =====
            if (currentStatus != Status.RUNNING) {
                throw new AuctionClosedException("Phiên_đang_đóng");
            }

            // ===== CHECK GIÁ =====
            if (newPrice <= currentPrice) {
                throw new InvalidBidException("Giá_mới_phải_cao_hơn_hiện_tại");
            }

            if (newPrice - currentPrice < 100) {
                throw new InvalidBidException("Bước_giá_tối_thiểu_là_100");
            }

            // ===== UPDATE =====
            currentPrice = newPrice;
            currentBidder = bidder;

            bidHistory.add(new BidTransaction(bidItem, bidder, newPrice));

            extendAuction();
            handleAutoBid();

            message = "NOTIFY " + id + " " + newPrice;

            if (!observers.contains(bidder)) {
                shouldAddObserver = true;
            }

        } finally {
            lock.unlock();
        }

        // ===== NGOÀI LOCK =====
        if (shouldAddObserver) {
            addObserver(bidder);
        }

        if (message != null) {
            notifyObservers(message);
        }
    }

    public void cancel() {
        lock.lock();
        try {
            transitionTo(Status.CANCELED);
            DatabaseManager.saveOrUpdateAuction(this); // Lưu khi phiên đấu giá bị hủy
        } finally {
            lock.unlock();
        }
    }

    public boolean pay() {

        lock.lock();
        try {

            //  chưa kết thúc
            if (currentStatus != Status.FINISH) {
                return false;
            }

            //  không có người thắng
            if (currentBidder == null) {
                return false;
            }

            double amount = currentPrice;

            //  không đủ tiền
            try {
                currentBidder.checkBalance(amount);
            } catch (Exception e) {
                return false;
            }

            // ===== TRỪ TIỀN =====
            currentBidder.setBalance(currentBidder.getBalance() - amount);

            // ===== CỘNG TIỀN =====
            seller.setBalance(seller.getBalance() + amount);

            // ===== CHUYỂN TRẠNG THÁI =====
            transitionTo(Status.PAID);

            System.out.println("PAY SUCCESS: " + amount);

            return true; //  thành công

        } finally {
            lock.unlock();
            // AuctionManager sẽ gọi saveOrUpdateAuction sau khi pay
        }
    }
    public void registerAutoBid(Bidder bidder, double maxPrice) {
        lock.lock();
        try {
            AutoBid existing = autoBidMap.get(bidder.getId());

            if (existing != null) {
                autoBidQueue.remove(existing);
                existing.setMaxPrice(maxPrice);
                autoBidQueue.add(existing);
            } else {
                AutoBid autoBid = new AutoBid(bidder, maxPrice);
                autoBidMap.put(bidder.getId(), autoBid);
                autoBidQueue.add(autoBid);
            }

        } finally {
            lock.unlock();
        }
    }
    private void handleAutoBid() {

        while (true) {

            AutoBid top;

            lock.lock();
            try {
                if (autoBidQueue.isEmpty()) return;

                top = autoBidQueue.peek();

                // nếu đã là người dẫn đầu → dừng
                if (top.getBidder().equals(currentBidder)) return;

                double nextPrice = currentPrice + MIN_INCREMENT;

                // vượt max → loại
                if (nextPrice > top.getMaxPrice()) {
                    autoBidQueue.poll();
                    autoBidMap.remove(top.getBidder().getId());
                    continue;
                }

                // không đủ tiền → loại
                try {
                    top.getBidder().checkBalance(nextPrice);
                } catch (Exception e) {
                    autoBidQueue.poll();
                    autoBidMap.remove(top.getBidder().getId());
                    continue;
                }

                // ===== AUTO BID =====
                currentPrice = nextPrice;
                currentBidder = top.getBidder();

                bidHistory.add(new BidTransaction(bidItem, currentBidder, currentPrice));

            } finally {
                lock.unlock();
            }

            notifyObservers("AUTO_BID " + id + " " + currentPrice);
        }
    }

    public long getRemainingTime() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }
}