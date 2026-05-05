package NetWork;

import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private Socket socket;
    private PrintWriter out;

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send(String message) {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            out.println(message);
            System.out.println("SEND >> " + message);
        } else {
            System.out.println("Chưa kết nối server!");
        }
    }

    // ===== CHỨC NĂNG =====

    public void login(String username, String password) {
        send("LOGIN " + username + " " + password);
    }

    public void placeBid(int auctionId, double price) {
        send("PLACE_BID " + auctionId + " " + price);
    }

    public void createAuction(String itemId, String sellerId, double startPrice) {
        itemId = itemId.replace(" ", "_"); // tránh lỗi split
        send("CREATE_AUCTION " + itemId + " " + sellerId + " " + startPrice);
    }

    public void getAuctions() {
        send("GET_AUCTIONS");
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
            if (out != null) out.close();
            System.out.println("Disconnected");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}