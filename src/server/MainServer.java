package server;

import server.network.ServerSocketRunner;
import server.repository.DatabaseManager;

public class MainServer {

    public static void main(String args[]) {
        DatabaseManager.loadEverything();

        ServerSocketRunner server = new ServerSocketRunner("0.0.0.0", 3636);
        server.start();
    }
}
