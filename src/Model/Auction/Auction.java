package Model.Auction;

import Controllers.Exceptions.AuctionClosedException;
import Controllers.Exceptions.InvalidBidException;
import Model.Item.Item;
import Model.User.Bidder;
import Model.User.Seller;
import Model.Observer.Observer;
import Controllers.Base.DatabaseManager; // Import DatabaseManager
import Model.User.UserManager;

import java.util.*;
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
    // ===== AUTO BID =====
    private final Map<Integer, AutoBid> autoBidMap = new HashMap<>();

    private final PriorityQueue<AutoBid> autoBids =
            new PriorityQueue<>((a, b) -> {
                if (a.getMaxBid() != b.getMaxBid()) {
                    return Double.compare(b.getMaxBid(), a.getMaxBid());
                }
                return Long.compare(a.getTimestamp(), b.getTimestamp());
            });




    private static final double MIN_INCREMENT = 100;

    private List<Observer> observers = new ArrayList<>();
    private int id;
    private Item bidItem;
    private Seller seller;
    private final List<BidTransaction> bidHistory = new ArrayList<>();
    public List<BidTransaction> getBidHistory() {
        return new ArrayList<>(bidHistory);
    }
    private double currentPrice;
    private Bidder currentBidder;



    private final ReentrantLock lock = new ReentrantLock();

    // ===== CONSTRUCTOR cho Auction mới =====
    public Auction(int id, Item bidItem, Seller seller, double startPrice, double currentPrice, Bidder currentBidder, Status status, long startTime, long endTime) {
        this.id = id;
        this.bidItem = bidItem;
        this.seller = seller;
        this.currentPrice = currentPrice;
        this.currentBidder = currentBidder;
        this.currentStatus = status;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // ===== CONSTRUCTOR DỰ PHÒNG (Dành cho AuctionManager gọi) =====
    public Auction(int id, Item bidItem, Seller seller, double startPrice) {
        // Nó sẽ tự động gọi cái Constructor 9 tham số, với startTime = 0 và endTime = 0
        this(id, bidItem, seller, startPrice, startPrice, null, Status.OPEN, 0, 0);
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

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    // helper cho server (rất nên có)
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
            case CANCELED:
                return next == Status.OPEN;
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

        DatabaseManager.saveOrUpdateAuction(this); //  THÊM
    }

    private void scheduleFinish() {
        long delay = Math.max(0, endTime - System.currentTimeMillis());
        finishTask = scheduler.schedule(() -> {
            boolean shouldNotify = false;
            lock.lock();
            try {
                // ***
                // nếu đã được extend thì task cũ không được finish nữa
                if (System.currentTimeMillis() < endTime) {
                    return;
                }
                if (currentStatus == Status.RUNNING) {
                    transitionTo(Status.FINISH);
                    System.out.println("Auction auto finished");
                    DatabaseManager.saveOrUpdateAuction(this);
                    shouldNotify = true;
                }
            } finally {
                lock.unlock();
            }
            // notify ngoài lock
            if (shouldNotify) {
                notifyObservers("STATUS_CHANGED " + id + " FINISH");
                notifyObservers("AUCTION_FINISHED " + id);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    private void extendAuction() {

        long now = System.currentTimeMillis();

        if (endTime < now) {
            endTime = now + EXTEND_TIME;
        } else {
            endTime += EXTEND_TIME;
        }

        if (finishTask != null) {
            finishTask.cancel(false);
        }

        scheduleFinish();

        DatabaseManager.saveOrUpdateAuction(this);
    }
    public void resumeAfterRestart() {

        long now = System.currentTimeMillis();

        // ✅ Nếu đã hết hạn
        if (endTime <= now && currentStatus == Status.RUNNING) {

            lock.lock();
            try {
                transitionTo(Status.FINISH);
                DatabaseManager.saveOrUpdateAuction(this);
            } finally {
                lock.unlock();
            }

            notifyObservers("STATUS_CHANGED " + id + " FINISH");
            notifyObservers("AUCTION_FINISHED " + id);

            return;
        }

        // ✅ Nếu chưa hết hạn → schedule lại
        if (currentStatus == Status.RUNNING) {
            scheduleFinish();
        }
    }
    public void forceFinish() {

        boolean shouldNotify = false;

        lock.lock();
        try {

            if (currentStatus == Status.RUNNING) {
                transitionTo(Status.FINISH);
                DatabaseManager.saveOrUpdateAuction(this);
                shouldNotify = true;
            }
        } finally {
            lock.unlock();
        }

        if (shouldNotify) {

            notifyObservers("AUCTION_FINISHED " + id);
        }
    }


    public void placeBid(double newPrice, Bidder bidder)
            throws AuctionClosedException, InvalidBidException {

        String message = null;
        boolean shouldAddObserver = false;

        lock.lock();
        try {

            // ===== CHECK BALANCE =====
            try {
                bidder.checkBalance(newPrice);
            } catch (Exception e) {
                throw new InvalidBidException("Không_đủ_số_dư");
            }

            // ===== CASE OPEN =====
            if (currentStatus == Status.OPEN) {

                if (newPrice <= currentPrice) {
                    throw new InvalidBidException("Giá_Không_hợp_lệ");
                }

                if (newPrice - currentPrice < MIN_INCREMENT) {
                    throw new InvalidBidException("Bước_giá_tối_thiểu_là_100");
                }
                // nhả tiền người cũ
                if (currentBidder != null) {
                    currentBidder.release(currentPrice);
                }

                // giữ tiền người mới
                bidder.reserve(newPrice);

                currentPrice = newPrice;
                currentBidder = bidder;

                bidHistory.add(new BidTransaction(bidder, newPrice));

                startAuction();
                processAutoBids();

                DatabaseManager.saveOrUpdateAuction(this);

                message = "NOTIFY " + id + " " + currentPrice;

                if (!observers.contains(bidder)) {
                    shouldAddObserver = true;
                }

            }
            // ===== CASE RUNNING =====
            else if (currentStatus == Status.RUNNING) {

                if (currentBidder != null &&
                        currentBidder.getId() == bidder.getId()) {
                    throw new InvalidBidException("Bạn_đang_là_người_giữ_giá_cao_nhất");
                }

                if (newPrice <= currentPrice) {
                    throw new InvalidBidException("Giá_mới_phải_cao_hơn_hiện_tại");
                }

                if (newPrice - currentPrice < MIN_INCREMENT) {
                    throw new InvalidBidException("Bước_giá_tối_thiểu_là_100");
                }
                // nhả tiền người cũ
                if (currentBidder != null) {
                    currentBidder.release(currentPrice);
                }

                // giữ tiền người mới
                bidder.reserve(newPrice);



                currentPrice = newPrice;
                currentBidder = bidder;

                bidHistory.add(new BidTransaction( bidder, newPrice));

                extendAuction();
                boolean changed = processAutoBids();

                if (changed) {
                    message = "NOTIFY " + id + " " + currentPrice;
                } else {
                    message = "NOTIFY " + id + " " + currentPrice;
                }



                if (!observers.contains(bidder)) {
                    shouldAddObserver = true;
                }
            }
            // ===== CASE KHÁC =====
            else {
                throw new AuctionClosedException("Phiên_đang_đóng");
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
            notifyObservers("STATUS_CHANGED " + id + " CANCELED");
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
            currentBidder.release(amount);
            currentBidder.setBalance(currentBidder.getBalance() - amount);

            // ===== CỘNG TIỀN =====
            seller.setBalance(seller.getBalance() + amount);

            // ===== CHUYỂN TRẠNG THÁI =====
            transitionTo(Status.PAID);
            notifyObservers("STATUS_CHANGED " + id + " PAID");

            System.out.println("PAY SUCCESS: " + amount);
            //lưu db
            DatabaseManager.saveOrUpdateAuction(this);
            return true; //  thành công

        } finally {
            lock.unlock();
            // AuctionManager sẽ gọi saveOrUpdateAuction sau khi pay
        }
    }


    public void setStatus(Status nextStatus) {
        lock.lock();
        try {
            // 1. Kiểm tra tính hợp lệ qua State Machine thông qua transitionTo
            transitionTo(nextStatus);

            // 2. Phát thông báo realtime tới toàn bộ các bên đang Observer (Client)
            notifyObservers("STATUS_CHANGED " + id + " " + nextStatus.name());

            // 3. Ghi dữ liệu đồng bộ xuống MySQL Database ngay lập tức
            DatabaseManager.saveOrUpdateAuction(this);

            System.out.println("Auction ID " + id + " chuyển trạng thái thành công sang: " + nextStatus);
        } finally {
            lock.unlock();
        }
    }




    // ===== HÀM RESET THỜI GIAN KHI ADMIN KHÔI PHỤC AUCTION =====
    public void resetAuctionTime() {
        lock.lock();
        try {
            this.startTime = 0;
            this.endTime = 0;

            // Nếu có task chạy ngầm tính thời gian cũ đang xếp hàng, hủy nó ngay
            if (finishTask != null) {
                finishTask.cancel(false);
            }

            System.out.println("Auction ID " + id + " đã xóa trắng mốc thời gian (Chờ lượt đặt giá mới để đếm ngược).");
        } finally {
            lock.unlock();
        }
    }
    public void registerAutoBid(Bidder bidder, double maxBid, double increment) {
        lock.lock();
        try {

            int bidderId = bidder.getId();

            // ❌ nếu đã có → không cho đăng ký lại
            if (autoBidMap.containsKey(bidderId)) {
                throw new IllegalStateException("Đã_đăng_ký_auto_bid");
            }

            if (maxBid <= currentPrice) {
                throw new IllegalArgumentException("MaxBid phải > currentPrice");
            }

            if (increment < MIN_INCREMENT) {
                increment = MIN_INCREMENT;
            }

            AutoBid ab = new AutoBid(bidder, maxBid, increment);

            //  add vào cả 2
            autoBidMap.put(bidderId, ab);
            autoBids.add(ab);



        } finally {
            lock.unlock();
        }
    }
    private boolean processAutoBids() {

        if (autoBids.isEmpty()) return false;

        AutoBid first = autoBids.poll();

        if (!autoBidMap.containsKey(first.getBidder().getId())) {
            return false;
        }

        AutoBid second = autoBids.peek();

        double nextPrice;

        if (second == null) {
            double inc = Math.max(first.getIncrement(), MIN_INCREMENT);
            nextPrice = Math.min(first.getMaxBid(), currentPrice + inc);
        } else {
            double inc = Math.max(first.getIncrement(), MIN_INCREMENT);
            nextPrice = Math.min(first.getMaxBid(), second.getMaxBid() + inc);
        }

        if (nextPrice <= currentPrice) {
            autoBids.add(first);
            return false;
        }

        if (currentBidder != null) {
            currentBidder.release(currentPrice);
        }

        first.getBidder().reserve(nextPrice);

        currentPrice = nextPrice;
        currentBidder = first.getBidder();

        bidHistory.add(new BidTransaction(currentBidder, currentPrice));

        autoBids.add(first);

        extendAuction();

        return true;
    }











    public long getRemainingTime() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }
}