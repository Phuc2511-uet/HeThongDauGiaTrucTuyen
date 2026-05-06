package Base;

import User.UserManager;

import java.io.IOException;

import User.UserManager;

import java.io.IOException;

    public class DatabaseManager {
        private static final String DATA_FILE = "server_data.dat";

        // load dữ liệu khi server bắt đầu khởi động
        public static void loadData() {
            try {
                Object data = FileHandler.load(DATA_FILE);
                if (data instanceof UserManager) {
                    // gán lại instance cho UserManager (Singleton)
                    UserManager.setInstance((UserManager) data);
                    System.out.println("Đã tải thành công dữ liệu từ Database");
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Không tìm thấy Database cũ. Khởi tạo hệ thống mới.");
                // nếu không có file, hệ thống sẽ tự dùng UserManager trống
            }
        }

        // lưu dữ liệu khi server tắt hoặc sau mỗi giao dịch
        public static void saveData() {
            try {
                FileHandler.save(UserManager.getInstance(), DATA_FILE);
                System.out.println("Đã lưu toàn bộ dữ liệu vào Database an toàn");
            } catch (IOException e) {
                System.err.println("Lỗi khi lưu dữ liệu: " + e.getMessage());
            }
        }
    }
