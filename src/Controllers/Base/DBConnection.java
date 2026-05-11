package Controllers.Base;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String HOST = "uet-auction-project-hethongdaugia.k.aivencloud.com";
    private static final String PORT = "26429";
    private static final String PASS = "AVNS_6tHG7bT60eOGY0waQFn";
    private static final String USER = "avnadmin";
    private static final String DB_NAME = "defaultdb";

    public static Connection getConnection() throws SQLException {

        String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?ssl-mode=REQUIRED";
        return DriverManager.getConnection(url, USER, PASS);
    }
}