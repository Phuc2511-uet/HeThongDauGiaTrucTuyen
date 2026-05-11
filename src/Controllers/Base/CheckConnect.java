package Controllers.Base;

import java.sql.Connection;

public class CheckConnect {
    public static void main(String[] args) {
        System.out.println("Đang thử kết nối tới Aiven...");
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                System.out.println(">>> KẾT NỐI THÀNH CÔNG RỒI!");
                System.out.println("Server: " + conn.getMetaData().getDatabaseProductVersion());
            }
        } catch (Exception e) {
            System.err.println("!!! LỖI RỒI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}