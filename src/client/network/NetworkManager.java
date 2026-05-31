package client.network;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = false;
    private Consumer<String> messageHandler;

    public void connect(String host, int port, Consumer<String> handler) throws IOException {
        this.messageHandler = handler;
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;
        startReceiveThread();
    }

    private void startReceiveThread() {
        new Thread(() -> {
            try {
                String message;
                while (running && (message = in.readLine()) != null) {
                    if (messageHandler != null) {
                        messageHandler.accept(message);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Network error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void send(String message) {
        if (isConnected()) {
            out.println(message);
        } else {
            System.err.println("Not connected to server!");
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            System.err.println("Error while disconnecting: " + e.getMessage());
        }
    }
}
