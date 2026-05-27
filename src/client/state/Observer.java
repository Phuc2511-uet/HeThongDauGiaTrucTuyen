package client.state;

public interface Observer {
    // Hàm được gọi khi có cập nhật mới
    void update(String message);
}