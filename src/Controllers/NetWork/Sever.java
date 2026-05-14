package Controllers.NetWork;

import java.io.*;
import java.net.*;

import Model.AuctionManager.AuctionManager;
import Model.User.*;

public class Sever {

    public static void main(String args[]) {
        Controllers.Base.DatabaseManager.loadEverything();
        String host = "0.0.0.0";
        int port = 3636;

        try {
            ServerSocket svsocket = new ServerSocket();
            svsocket.bind(new InetSocketAddress(host, port));



            while (true) {
                Socket socket = svsocket.accept();

                // mỗi client chạy 1 thread riêng
                new Thread(() -> handleClient(socket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void handleClient(Socket socket) {

        User currentUser = null; // lưu user của client này

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {

            String message;
            InformationHandle handle = InformationHandle.getInstance();

            while ((message = in.readLine()) != null) {

                System.out.println("Received: " + message);

                String[] parts = message.split(" ");
                String action = parts[0];

                // 1. XỬ LÝ ĐĂNG KÝ (Cho phép thực hiện khi chưa login)

                if (action.equals("NEW_ACCOUNT")) {
                    String response = handle.handleIfo(message, currentUser);
                    out.println(response);
                    continue;
                }
                // 2. XỬ LÝ LOGIN
                if (action.equals("LOGIN")) {
                    try {
                        currentUser = UserManager.getInstance().authenticate(parts[1], parts[2]);
                        String role = "UNKNOWN";
                        double balance = 0.0; // Mặc định cho Admin

                        if (currentUser instanceof Bidder) {
                            role = "BIDDER";
                            balance = ((Bidder) currentUser).getBalance();
                        }
                        else if (currentUser instanceof Seller) {
                            role = "SELLER";
                            balance = ((Seller) currentUser).getBalance();
                        }
                        else if (currentUser instanceof Admin) {
                            role = "ADMIN";
                            balance = 0.0;
                        }

                        // Gửi về Client    LOGIN_SUCCESS <ROLE> <FULLNAME> <BALANCE>
                        String response = String.format("LOGIN_SUCCESS %s %s %.2f %s",
                                role,
                                currentUser.getFullName().replace(" ", "_"),
                                balance,
                                currentUser.getUsername()
                        );
                        out.println(response);
                    } catch (Exception e) {
                        out.println("LOGIN_FAILED");
                    }
                    continue;
                }
                // ===== LOGOUT =====
                if (action.equals("LOGOUT")) {

                    if (currentUser == null) {
                        out.println("ERROR Not logged in");
                        continue;
                    }

                    // nếu là bidder thì xóa connection
                    if (currentUser instanceof Bidder) {
                        ((Bidder) currentUser).setConnection(null);
                    }

                    currentUser = null; //  logout thật sự

                    out.println("LOGOUT_SUCCESS");
                    continue;
                }

                //=====DEPOSIT======
                if (action.equals("DEPOSIT")) {
                    try {
                        double amount = Double.parseDouble(parts[1]);

                        if (currentUser instanceof Bidder) {
                            Bidder bidder = (Bidder) currentUser;

                            // Gọi hàm deposit có sẵn trong file Bidder.java của bạn
                            if (bidder.deposit(amount)) {
                                // Trả về số dư mới sau khi đã cộng thành công
                                out.println("DEPOSIT_SUCCESS " + bidder.getBalance());
                            } else {
                                out.println("DEPOSIT_FAILED So_tien_phai_lon_hon_0");
                            }
                        } else {
                            out.println("DEPOSIT_FAILED Chi_Bidder_moi_co_the_nap_tien");
                        }
                    } catch (Exception e) {
                        out.println("DEPOSIT_FAILED Loi_dinh_dang_so_tien");
                    }
                    continue;
                }


                // 3. CHẶN CÁC LỆNH KHÁC NẾU CHƯA LOGIN
                if (currentUser == null) {
                    out.println("ERROR Not logged in");
                    continue;
                }

                // ===== XỬ LÝ REQUEST KHÁC =====
                String response = handle.handleIfo(message, currentUser);

                out.println(response);
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket);
        }finally {
            if (currentUser instanceof Bidder) {
                ((Bidder) currentUser).setConnection(null);
            }
        }
    }


}