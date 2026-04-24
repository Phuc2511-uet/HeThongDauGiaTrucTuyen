package Auction;

import Item.*;
import Observer.Observer;
import User.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Auction {

    enum Status {
        OPEN,
        RUNNING,
        FINISH,
        PAID,
        CANCELED
    }

    private Status currentStatus;

    private List<Observer> observers = new ArrayList<>();
    private int id;
    private Item bidItem;
    private Seller seller;
    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private double currentPrice;
    private Bidder currentBidder;

    //thêm Observer
    public synchronized void addObserver(Observer observer) {
        observers.add(observer);
    }
    //xóa Observer
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    //Thông báo cho tất cả Observer trong list
    public synchronized void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }

    // đặt giá mới
    public void placeBid(double newPrice, Bidder bidder) {
        if (newPrice > currentPrice) {
            this.currentPrice = newPrice;
            this.currentBidder = bidder;
            notifyObservers("Giá mới cho sản phẩm " + bidItem.getId() + " là: " + newPrice);
        }
    }

    // ===== TIME =====
    private long startTime;
    private long endTime;

    private static final long DURATION = 60 * 60 * 1000; // 1 giờ
    private static final long EXTEND_TIME = 60 * 1000;   // +1 phút

    // ===== SCHEDULER =====
    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> finishTask;

    // ===== LOCK =====
    private final ReentrantLock lock = new ReentrantLock();

    // ===== CONSTRUCTOR =====
    public Auction(int id, Item bidItem, Seller seller, double startPrice) {
        this.id = id;
        this.bidItem = bidItem;
        this.seller = seller;
        this.currentPrice = startPrice;
        this.currentStatus = Status.OPEN;
    }

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
    }

    // ===== START AUCTION =====
    private void startAuction() {
        long now = System.currentTimeMillis();

        startTime = now;
        endTime = now + DURATION;

        transitionTo(Status.RUNNING);

        scheduleFinish();
    }

    // ===== SCHEDULE FINISH =====
    private void scheduleFinish() {
        long delay = Math.max(0, endTime - System.currentTimeMillis());

        finishTask = scheduler.schedule(() -> {
            lock.lock();
            try {
                if (currentStatus == Status.RUNNING) {
                    transitionTo(Status.FINISH);
                    System.out.println("Auction auto finished");
                }
            } finally {
                lock.unlock();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    // ===== EXTEND TIME =====
    private void extendAuction() {
        endTime += EXTEND_TIME;

        if (finishTask != null) {
            finishTask.cancel(false);
        }

        scheduleFinish();
    }

    public void newPrice(double newPrice, Bidder bidder) {

        lock.lock();
        try {

            // ===== BID ĐẦU =====
            if (currentStatus == Status.OPEN) {

                if (newPrice <= currentPrice) {
                    throw new IllegalArgumentException("Must be higher than start price");
                }

                currentPrice = newPrice;
                currentBidder = bidder;

                // 👉 ADD HISTORY
                bidHistory.add(new BidTransaction(bidItem, bidder, newPrice));

                startAuction();
                return;
            }

            // ===== CHECK STATUS =====
            if (currentStatus != Status.RUNNING) {
                throw new IllegalStateException("Auction not running");
            }

            // ===== VALIDATE =====
            if (newPrice <= currentPrice) {
                throw new IllegalArgumentException("Must be higher");
            }

            if (newPrice - currentPrice < 100) {
                throw new IllegalArgumentException("Min increment 100");
            }

            // ===== UPDATE =====
            currentPrice = newPrice;
            currentBidder = bidder;

            // 👉 ADD HISTORY (QUAN TRỌNG: sau khi update thành công)
            bidHistory.add(new BidTransaction(bidItem, bidder, newPrice));

            // ===== EXTEND =====
            extendAuction();

        } finally {
            lock.unlock();
        }
    }

    // ===== MANUAL ACTIONS =====
    public void cancel() {
        lock.lock();
        try {
            transitionTo(Status.CANCELED);
        } finally {
            lock.unlock();
        }
    }

    public void pay() {
        lock.lock();
        try {
            transitionTo(Status.PAID);
        } finally {
            lock.unlock();
        }
    }

    // ===== HELPER =====
    public long getRemainingTime() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }
}