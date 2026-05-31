package client.network;

import client.state.Observer;
import java.io.IOException;
import java.util.List;

/**
 * Facade class for client network operations.
 * Delegates to specialized components: NetworkManager, ClientSession, and MessageDispatcher.
 */
public class ClientConnection {
    private static ClientConnection instance;

    private final NetworkManager networkManager;
    private final ClientSession session;
    private final MessageDispatcher dispatcher;

    // Static ID for UI selection
    public static int selectedAuctionId;

    private ClientConnection() {
        this.networkManager = new NetworkManager();
        this.session = new ClientSession();
        this.dispatcher = new MessageDispatcher(session);
    }

    public static synchronized ClientConnection getInstance() {
        if (instance == null) {
            instance = new ClientConnection();
        }
        return instance;
    }

    // ===== CONNECTION =====
    public void connect(String host, int port) {
        try {
            networkManager.connect(host, port, dispatcher::handleMessage);
            System.out.println("Connected to server: " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        networkManager.disconnect();
        System.out.println("Disconnected from server.");
    }

    // ===== SEND DATA =====
    public void send(String message) {
        networkManager.send(message);
    }

    // ===== SESSION & OBSERVERS =====
    public void addObserver(Observer obs) { session.addObserver(obs); }
    public void removeObserver(Observer obs) { session.removeObserver(obs); }
    public List<Observer> getObservers() { return session.getObservers(); }

    public String getCurrentRole() { return session.getCurrentRole(); }
    public String getCurrentFullname() { return session.getCurrentFullname(); }
    public double getCurrentBalance() { return session.getCurrentBalance(); }
    public String getCurrentUsername() { return session.getCurrentUsername(); }

    // ===== AUTO-BID MANAGEMENT =====
    public boolean isAutoBidActivatedForAuction(int auctionId) {
        return session.isAutoBidActivatedForAuction(auctionId);
    }
    public void addActivatedAutoBidAuction(int auctionId) {
        session.addActivatedAutoBidAuction(auctionId);
    }

    // ===== RECENT REFACTORED COMMANDS =====
    public void login(String username, String password) {
        send("LOGIN " + username + " " + password);
    }

    public void logOut() {
        send("LOGOUT");
        session.reset();
    }

    public void getMyItems() { send("GET_MY_ITEMS"); }
    public void getWonAuctions() { send("GET_WON_AUCTIONS"); }
    public void getSellerAuctions() { send("GET_SELLER_AUCTIONS"); }
    public void getUserById(int userId) { send("GET_USER_BY_ID " + userId); }
    public void updateItemPrice(int itemId, double newPrice) { send("UPDATE_ITEM_PRICE " + itemId + " " + newPrice); }
    public void getItemIds() { send("GET_ITEM_IDS"); }
    public void deleteUser(int userId) { send("DELETE_USER " + userId); }
    public void getUserIds() { send("GET_USER_IDS"); }
    public void getAuctionById(int id) { send("GET_AUCTION_BY_ID " + id); }
    public void getCurrentUser() { send("GET_CURRENT_USER"); }
    public void createItem(String type, String name, double price, String imageBase64) {
        send("CREATE_ITEM " + type + " " + name.replace(" ", "_") + " " + price + " " + imageBase64);
    }
    public void placeBid(int auctionId, double price) { send("PLACE_BID " + auctionId + " " + price); }
    public void createAuction(String itemId, String sellerId, double startPrice) {
        send("CREATE_AUCTION " + itemId + " " + sellerId + " " + startPrice);
    }
    public void newAccount(String username, String password, String role, String fullname) {
        send("NEW_ACCOUNT " + username + " " + password + " " + role + " " + fullname.replace(" ", "_"));
    }
    public void deposit(double amount) { send("DEPOSIT " + amount); }
    public void getAuctions() { send("GET_AUCTIONS"); }
    public void getItemById(int id) { send("GET_ITEM_BY_ID " + id); }
    public void deleteItem(int itemId) { send("DELETE_ITEM " + itemId); }
    public void sellerDeleteItem(int itemId) { send("SELLER_DELETE_ITEM " + itemId); }
}
