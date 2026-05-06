package NetWork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

public class Client {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private volatile boolean running = false;

    // ===== KẾT NỐI =====
    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            running = true;

            System.out.println("Connected to server");

            startReceiveThread();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== LUỒNG NHẬN DỮ LIỆU =====
    private void startReceiveThread() {
        new Thread(() -> {
            try {
                String message;

                while (running && (message = in.readLine()) != null) {
                    handleServerMessage(message);
                }

            } catch (Exception e) {
                System.out.println("Receive error: " + e.getMessage());
            }
        }).start();
    }

    // ===== XỬ LÝ MESSAGE TỪ SERVER =====
    private void handleServerMessage(String message) {

        System.out.println("SERVER >> " + message);

        // ===== LIST AUCTION =====
        if (message.startsWith("LIST_AUCTION")) {

            String[] parts = message.split(" ");

            List<Integer> auctionIds = new ArrayList<>();

            for (int i = 1; i < parts.length; i++) {
                try {
                    auctionIds.add(Integer.parseInt(parts[i]));
                } catch (Exception e) {
                    System.out.println("Invalid ID: " + parts[i]);
                }
            }

            onReceiveAuctionList(auctionIds);
            return;
        }

        // ===== MESSAGE KHÁC =====
        switch (message) {
            case "LOGIN_SUCCESS":
                System.out.println("Đăng nhập thành công");
                break;

            case "LOGIN_FAILED":
                System.out.println("Sai tài khoản hoặc mật khẩu");
                break;

            case "BID_SUCCESS":
                System.out.println("Đặt giá thành công");
                break;

            case "ACCOUNT_SUCCESS":
                System.out.println("Tạo tài khoản thành công");
                break;

            case "ACCOUNT_FAILED":
                System.out.println("Tạo tài khoản thất bại");
                break;

            default:
                if (message.startsWith("ERROR")) {
                    System.out.println("Lỗi: " + message);
                } else {
                    System.out.println("Unknown: " + message);
                }
        }
    }




    // ===== GỬI DỮ LIỆU =====
    private void send(String message) {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            out.println(message);
            out.println("SEND >> " + message);
        } else {
            out.println("Chưa kết nối server!");
        }
    }

    // ===== CHỨC NĂNG =====
    public void getAuctionById(String id){
        send("GET_AUCTION_BY_ID" + " " + id);

    }
    public void createItem(String name, double price) {

        send("CREATE_ITEM " + name + " " + price);
    }

    public void login(String username, String password) {
        send("LOGIN " + username + " " + password);
    }

    public void placeBid(int auctionId, double price) {
        send("PLACE_BID " + auctionId + " " + price);
    }

    public void createAuction(String itemId, String sellerId, double startPrice) {

        send("CREATE_AUCTION " + itemId + " " + sellerId + " " + startPrice);
    }
    public void newAccount(String username, String password, String role,String fullname) {
        send("NEW_ACCOUNT " + username + " " + password + " " + role+" " + fullname);
    }

    public void getAuctions() {
        send("GET_AUCTIONS");
    }

    // ===== NGẮT KẾT NỐI =====
    public void disconnect() {
        try {
            running = false;

            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();

            out.println("Disconnected");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void onReceiveAuctionList(List<Integer> list) {

        System.out.println("=== DANH SÁCH AUCTION ===");

        for (int id : list) {
            System.out.println("Auction ID: " + id);
        }

        // sau này  thay bằng update GUI
    }
}