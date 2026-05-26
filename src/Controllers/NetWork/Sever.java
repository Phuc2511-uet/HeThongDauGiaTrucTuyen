package Controllers.NetWork;

import java.io.*;
import java.net.*;

import Model.AuctionManager.AuctionManager;
import Model.User.*;

public class Sever {

    public static void main(String args[]) {
        Controllers.Base.DatabaseManager.loadEverything();
        AuctionManager.getInstance().restoreAuctions();

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
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();

                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                PrintWriter out = new PrintWriter(os, true);

                DataInputStream dis = new DataInputStream(is);
                DataOutputStream dos = new DataOutputStream(os);


        ) {

            String message;
            InformationHandle handle = InformationHandle.getInstance();

            while ((message = in.readLine()) != null) {

                System.out.println("Received: " + message);
                // ===== UPLOAD IMAGE =====
                if (message.equals("UPLOAD_IMAGE")) {
                    try {
                        // 1. đọc tên file
                        String fileName = dis.readUTF();

                        // 2. đọc dữ liệu ảnh
                        int length = dis.readInt();
                        byte[] bytes = new byte[length];
                        dis.readFully(bytes);

                        // 3. tạo thư mục nếu chưa tồn tại
                        File folder = new File("images");
                        if (!folder.exists()) {
                            folder.mkdirs();
                        }

                        // 4. tạo tên file unique
                        String newName = "item_" + System.currentTimeMillis() + ".jpg";
                        String path = "images/" + newName;

                        // 5. lưu file
                        FileOutputStream fos = new FileOutputStream(path);
                        fos.write(bytes);
                        fos.close();

                        System.out.println("Saved image: " + path);

                        // 6. trả về path cho client
                        dos.writeUTF("IMAGE_PATH " + path);
                        dos.flush();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    continue;
                }

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
                        if (currentUser instanceof Bidder){
                            ((Bidder) currentUser).setConnection(out);
                        }
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
                        // Gửi về: LOGIN_SUCCESS <ROLE> <FULLNAME> <BALANCE> <USERNAME>
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