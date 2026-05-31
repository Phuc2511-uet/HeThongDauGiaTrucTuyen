package server.network;

import shared.model.user.Admin;
import shared.model.user.Bidder;
import shared.model.user.Seller;
import shared.model.user.User;
import server.repository.UserManager;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private User currentUser;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        handleClient();
    }

    public void handleClient() {
        try (
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                PrintWriter out = new PrintWriter(os, true);
                DataInputStream dis = new DataInputStream(is);
                DataOutputStream dos = new DataOutputStream(os)
        ) {
            // Khởi tạo thuộc tính PrintWriter toàn cục cho luồng này
            this.out = new PrintWriter(os, true);

            InformationHandle handle = InformationHandle.getInstance();
            String message;

            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);

                String response = handleTextRequest(message, handle, out);
                if (response != null) {
                    // ===== 3. KÍCH HOẠT THEO DÕI VÀO/RA PHÒNG TẠI ĐÂY =====
                    handleRoomViewerTracker(message, response);
                    out.println(response);
                }
            }
        } catch (IOException e) {
            System.out.println("ClientConnection disconnected: " + socket);
        } finally {
            // ===== 4. DỌN DẸP AN TOÀN KHI MẤT KẾT NỐI ĐỘT NGỘT =====
            cleanUpLeftRoom();

            clearBidderConnection();
        }
    }

    // ===== 5. HÀM TỰ ĐỘNG ĐIỀU PHỐI DANH SÁCH VIEWERS THEO SỰ KIỆN MẠNG =====
    private void handleRoomViewerTracker(String request, String response) {
        if (request == null || response == null) return;

        String[] parts = request.trim().split("\\s+");
        if (parts.length == 0) return;

        String action = parts[0];

        // Trường hợp A: Vào phòng thành công (Dựa trên phản hồi từ Service)
        if (response.startsWith("AUCTION_DETAIL_SUCCESS")) {
            try {
                String[] resParts = response.split("\\s+");
                int auctionId = Integer.parseInt(resParts[1]);

                // Nếu đang xem dở một phòng khác mà nhảy thẳng sang phòng này, xóa khỏi phòng cũ trước
                if (this.currentViewingAuctionId != -1 && this.currentViewingAuctionId != auctionId) {
                    cleanUpLeftRoom();
                }

                this.currentViewingAuctionId = auctionId;
                shared.model.auction.Auction currentAuction = server.repository.AuctionManager.getInstance().getAuctionById(auctionId);
                if (currentAuction != null) {
                    currentAuction.addViewer(this); // Thêm luồng này vào danh sách người xem phòng
                }
            } catch (Exception e) {
                System.err.println("Lỗi addViewer: " + e.getMessage());
            }
        }
        // Trường hợp B: Chủ động thoát phòng (Bấm Back từ Client)
        else if (action.equals("LEAVE_AUCTION")) {
            cleanUpLeftRoom();
        }
        // Trường hợp C: Bấm Menu chuyển tab hoặc Logout ngoài sảnh khi đang treo trong phòng
        else if (action.equals("GET_AUCTIONS") ||
                action.equals("GET_WON_AUCTIONS") ||
                action.equals("GET_SELLER_AUCTIONS") ||
                action.equals("LOGOUT")) {
            cleanUpLeftRoom();
        }
    }

    // Hàm phụ trợ bốc Client ra khỏi Set của phòng đấu giá
    private void cleanUpLeftRoom() {
        if (this.currentViewingAuctionId != -1) {
            shared.model.auction.Auction oldAuction = server.repository.AuctionManager.getInstance().getAuctionById(this.currentViewingAuctionId);
            if (oldAuction != null) {
                oldAuction.removeViewer(this); // Xóa khỏi Set viewers
            }
            this.currentViewingAuctionId = -1; // Trả về trạng thái sảnh chờ
        }
    }

    private String handleTextRequest(String message, InformationHandle handle, PrintWriter out) {
        String[] parts = message.split(" ");
        String action = parts[0];

        if (action.equals("NEW_ACCOUNT")) {
            return handle.handleIfo(message, currentUser);
        }

        if (action.equals("LOGIN")) {
            return handleLogin(parts, out);
        }

        if (action.equals("LOGOUT")) {
            return handleLogout();
        }

        if (currentUser == null) {
            return "ERROR Not logged in";
        }

        return handle.handleIfo(message, currentUser);
    }

    private String handleLogin(String[] parts, PrintWriter out) {
        try {
            currentUser = UserManager.getInstance().authenticate(parts[1], parts[2]);
            if (currentUser instanceof Bidder) {
                ((Bidder) currentUser).setConnection(out);
            }

            String role = "UNKNOWN";
            double balance = 0.0;

            if (currentUser instanceof Bidder) {
                role = "BIDDER";
                balance = ((Bidder) currentUser).getBalance();
            } else if (currentUser instanceof Seller) {
                role = "SELLER";
                balance = ((Seller) currentUser).getBalance();
            } else if (currentUser instanceof Admin) {
                role = "ADMIN";
            }

            return String.format("LOGIN_SUCCESS %s %s %.2f %s",
                    role,
                    currentUser.getFullName().replace(" ", "_"),
                    balance,
                    currentUser.getUsername()
            );
        } catch (Exception e) {
            return "LOGIN_FAILED";
        }
    }

    private String handleLogout() {
        if (currentUser == null) {
            return "ERROR Not logged in";
        }

        clearBidderConnection();
        currentUser = null;

        return "LOGOUT_SUCCESS";
    }

    private void clearBidderConnection() {
        if (currentUser instanceof Bidder) {
            ((Bidder) currentUser).setConnection(null);
        }
    }

    private void handleImageUpload(DataInputStream dis, DataOutputStream dos) {
        try {
            String fileName = dis.readUTF();
            int length = dis.readInt();
            byte[] bytes = new byte[length];
            dis.readFully(bytes);

            File folder = new File("images");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String newName = "item_" + System.currentTimeMillis() + ".jpg";
            String path = "images/" + newName;

            try (FileOutputStream fos = new FileOutputStream(path)) {
                fos.write(bytes);
            }

            System.out.println("Saved image: " + path + " from " + fileName);
            dos.writeUTF("IMAGE_PATH " + path);
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ===== xử lí danh sách viewer =====
    private PrintWriter out;
    private int currentViewingAuctionId = -1;
    // Lưu ID phòng đang xem (-1 là đang ở ngoài sảnh)

    // ===== ĐẨY DATA REAL-TIME XUỐNG CLIENT =====
    public void sendRawMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
