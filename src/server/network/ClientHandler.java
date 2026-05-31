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
            InformationHandle handle = InformationHandle.getInstance();
            String message;

            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);

                String response = handleTextRequest(message, handle, out);
                if (response != null) {
                    out.println(response);
                }
            }
        } catch (IOException e) {
            System.out.println("ClientConnection disconnected: " + socket);
        } finally {
            clearBidderConnection();
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
}
