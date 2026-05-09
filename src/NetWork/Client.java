package NetWork;

import Controller.MainFx;

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

    private String currentRole;
    private String currentFullname;
    private double currentBalance;

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

        // ===== SPLIT =====
        String[] parts = message.split("\\s+");
        String command = parts[0];

        List<String> data = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            data.add(parts[i]);
        }

        // ===== XỬ LÝ =====
        switch (command) {
            case "AUCTION_DETAIL":

            case "LIST_AUCTION":

            case "ITEM_IDS":

            case "ITEM_DETAIL":

            case "USER_IDS":

            case "USER_DETAIL":

            case "SELLER_AUCTIONS":

            case "WON_AUCTIONS":

            case "LOGIN_SUCCESS":{
                if (parts.length >= 4) {
                    this.currentRole = parts[1];
                    this.currentFullname = parts[2].replace("_", " ");
                    this.currentBalance = Double.parseDouble(parts[3]);

                    javafx.application.Platform.runLater(() -> {
                        MainFx.showHomeByRole(this.currentRole);
                    });
                }
                break;
            }
            case "LOGIN_FAILED":{
                javafx.application.Platform.runLater(() -> {
                    showAlert("Đăng nhập thất bại", "Sai tài khoản hoặc mật khẩu!");
                });
                break;
            }
            case "BID_SUCCESS":
            case "BID_FAILED":
            case "ACCOUNT_SUCCESS":{
                javafx.application.Platform.runLater(() -> {
                    showAlert("Thông báo", "Tạo tài khoản thành công! Vui lòng đăng nhập.");
                    MainFx.showLoginScene();
                });
                break;
            }
            case "ACCOUNT_FAILED":{
                javafx.application.Platform.runLater(() -> {
                    showAlert("Lỗi", "Tạo tài khoản thất bại! Tên đăng nhập có thể đã tồn tại.");
                });
                break;
            }
            case "UPDATE_PRICE_SUCCESS":
            case "UPDATE_PRICE_FAILED":
            case "DELETE_ITEM_SUCCESS":
            case "DELETE_ITEM_FAILED":
            case "DELETE_USER_SUCCESS":
            case "DELETE_USER_FAILED":
            case "DEPOSIT_SUCCESS":
            case "DEPOSIT_FAILED":

                System.out.println(command);
                break;

            case "ERROR":
                System.out.println("Lỗi: " + String.join(" ", data));
                break;

            default:
                System.out.println("Unknown: " + message);
        }
    }



    // ===== GỬI DỮ LIỆU =====
    private void send(String message) {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {

            out.println(message);
        } else {
            out.println("Chưa kết nối server!");
        }
    }

    // ===== CHỨC NĂNG =====
    public void getWonAuctions() {
        send("GET_WON_AUCTIONS");
    }
    public void getSellerAuctions() {
        send("GET_SELLER_AUCTIONS");
    }

    public void getUserById(int userId) {
        send("GET_USER_BY_ID " + userId);
    }
    public void updateItemPrice(int itemId, double newPrice) {
        send("UPDATE_ITEM_PRICE " + itemId + " " + newPrice);
    }
    public void getItemIds() {
        send("GET_ITEM_IDS");
    }

    public void deleteUser(int userId) {
        send("DELETE_USER " + userId);
    }

    public void getUserIds() {
        send("GET_USER_IDS");
    }
    public void getAuctionById(String id){
        send("GET_AUCTION_BY_ID" + " " + id);

    }
    public void createItem(String type, String name, double price) {

        send("CREATE_ITEM " + type + " " + name.replace(" ", "_") + " " + price);
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
    public void getItemById(int id) {
        send("GET_ITEM_BY_ID " + id);
    }
    public void logOut(){
        send("LOGOUT");
    }





    // ===== NGẮT KẾT NỐI =====
    public void disconnect() {
        running = false;
        try {
            // Đóng socket trước để phá vỡ trạng thái blocking của readLine()
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            System.out.println("Client disconnected safely.");
        } catch (Exception e) {
            System.err.println("Error while disconnecting: " + e.getMessage());
        }
    }
    private void onReceiveAuctionList(List<Integer> list) {

        System.out.println("=== DANH SÁCH AUCTION ===");

        for (int id : list) {
            System.out.println("Auction ID: " + id);
        }

        // sau này  thay bằng update GUI
    }

    //ngăn không cho soket tạo mới khi chuyển màn hình
    private static Client instance;
    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    //thông báo
    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


}