package client.network;

import client.state.Observer;
import java.util.ArrayList;
import java.util.List;

public class ClientSession {
    private String currentRole;
    private String currentFullname;
    private double currentBalance;
    private String currentUsername;
    private List<Observer> observers = new ArrayList<>();
    private List<Integer> activatedAutoBidAuctions = new ArrayList<>();

    public String getCurrentRole() { return currentRole; }
    public void setCurrentRole(String currentRole) { this.currentRole = currentRole; }

    public String getCurrentFullname() { return currentFullname; }
    public void setCurrentFullname(String currentFullname) { this.currentFullname = currentFullname; }

    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }

    public String getCurrentUsername() { return currentUsername; }
    public void setCurrentUsername(String currentUsername) { this.currentUsername = currentUsername; }

    public List<Observer> getObservers() { return observers; }

    public void addObserver(Observer obs) {
        if (!observers.contains(obs)) {
            observers.add(obs);
        }
    }

    public void removeObserver(Observer obs) {
        observers.remove(obs);
    }

    public void clearObservers() {
        observers.clear();
    }

    public void notifyObservers(String cmd) {
        for (Observer obs : new ArrayList<>(observers)) {
            obs.update(cmd);
        }
    }

    public boolean isAutoBidActivatedForAuction(int auctionId) {
        return activatedAutoBidAuctions.contains(auctionId);
    }

    public void addActivatedAutoBidAuction(int auctionId) {
        if (!activatedAutoBidAuctions.contains(auctionId)) {
            activatedAutoBidAuctions.add(auctionId);
        }
    }

    public void clearAutoBidAuctions() {
        activatedAutoBidAuctions.clear();
    }

    public void reset() {
        currentRole = "";
        currentFullname = "";
        currentBalance = 0.0;
        currentUsername = "";
        clearObservers();
        clearAutoBidAuctions();
    }
}
