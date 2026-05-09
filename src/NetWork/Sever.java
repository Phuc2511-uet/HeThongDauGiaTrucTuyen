package NetWork;

import java.io.*;
import java.net.*;
import User.*;

public class Sever {

    public static void main(String args[]) {
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

                // ===== LOGIN =====
                if (action.equals("LOGIN")) {
                    try {
                        String username = parts[1];
                        String password = parts[2];

                        currentUser = UserManager.getInstance()
                                .authenticate(username, password);
                        if (currentUser instanceof Bidder) {
                            ((Bidder) currentUser).setConnection(out);
                        }

                        out.println("LOGIN_SUCCESS");

                    } catch (Exception e) {
                        out.println("LOGIN_FAIL");
                    }

                    continue; //  không đi xuống dưới
                }

                // ===== CHƯA LOGIN =====
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