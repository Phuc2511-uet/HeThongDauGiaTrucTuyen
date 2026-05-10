package Auction;

import Item.*;
import Observer.Observer;
import User.*;

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

    private List<Observer> observers = new ArrayList<>();
    private int id;
    private Item bidItem;
    private Seller seller;
    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private double currentPrice;
    private Bidder currentBidder;



    private final ReentrantLock lock = new ReentrantLock();

    // ===== CONSTRUCTOR =====
    public Auction(int id,Item bidItem, Seller seller, double startPrice) {
        this.id = id;
        this.bidItem = bidItem;
        this.seller = seller;
        this.currentPrice = startPrice;
        this.currentStatus = Status.OPEN;
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
    public void placeBid(double newPrice, Bidder bidder) {

        String message = null;
        boolean shouldAddObserver = false;

        lock.lock();
        try {

            if (currentStatus == Status.OPEN) {

                if (newPrice <= currentPrice) {
                    throw new IllegalArgumentException("Must be higher than start price");
                }

                currentPrice = newPrice;
                currentBidder = bidder;

                bidHistory.add(new BidTransaction(bidItem, bidder, newPrice));

                startAuction();

                if (!observers.contains(bidder)) {
                    shouldAddObserver = true;
                }

                return;
            }

            if (currentStatus != Status.RUNNING) {
                throw new IllegalStateException("Auction not running");
            }

            if (newPrice <= currentPrice) {
                throw new IllegalArgumentException("Must be higher");
            }

            if (newPrice - currentPrice < 100) {
                throw new IllegalArgumentException("Min increment 100");
            }

            currentPrice = newPrice;
            currentBidder = bidder;

            bidHistory.add(new BidTransaction(bidItem, bidder, newPrice));

            extendAuction();

            message = "NOTIFY " + id + " " + newPrice;

            if (!observers.contains(bidder)) {
                shouldAddObserver = true;
            }

        } finally {
            lock.unlock();
        }

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
        } finally {
            lock.unlock();
        }
    }

    public boolean pay() {

        lock.lock();
        try {

            // ❌ chưa kết thúc
            if (currentStatus != Status.FINISH) {
                return false;
            }

            // ❌ không có người thắng
            if (currentBidder == null) {
                return false;
            }

            double amount = currentPrice;

            // ❌ không đủ tiền
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
        }
    }
    public long getRemainingTime() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }
}