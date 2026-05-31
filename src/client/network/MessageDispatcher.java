package client.network;

import client.MainFx;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.List;

public class MessageDispatcher {
    private final ClientSession session;

    public MessageDispatcher(ClientSession session) {
        this.session = session;
    }

    public void handleMessage(String message) {
        System.out.println("SERVER >> " + message);

        String[] parts = message.split("\\s+");
        String command = parts[0];

        switch (command) {

            case "ADMIN_USER_DETAIL":
                session.notifyObservers(message);
                break;

            case "USER_DETAIL":
                handleUserDetail(parts, message);
                break;

            case "LOGIN_SUCCESS":
                handleLoginSuccess(parts);
                break;

            case "LOGIN_FAILED":
                Platform.runLater(() -> showAlert("Đang nhập thất bại", "Sai tài khoản hoặc mật khu!"));
                break;

            case "LOGOUT_SUCCESS":
                handleLogoutSuccess();
                break;

            case "BID_SUCCESS":
                Platform.runLater(() -> showAlert("Thành công", "Đặt giá thành công!"));
                break;

            case "BID_FAILED":
                handleBidFailed(message);
                break;

            case "ACCOUNT_SUCCESS":
                Platform.runLater(() -> {
                    showAlert("Thông báo", "Tạo tài khoản thành công! Vui lòng đăng nhập.");
                    MainFx.showLoginScene();
                });
                break;

            case "ACCOUNT_FAILED":
                Platform.runLater(() -> showAlert("Lỗi", "Tạo tài khoản thất bại! Tên đăng nhập có thể đã tồn tại."));
                break;

            case "DELETE_USER_SUCCESS":
                session.notifyObservers(message);
                Platform.runLater(() -> showAlert("Thành công", "Xóa user thành công!"));
                break;

            case "DELETE_USER_FAILED":
                session.notifyObservers(message);
                Platform.runLater(() -> showAlert("Lỗi", "Xóa user thất bại!"));
                break;

            case "DELETE_ITEM_SUCCESS":
                session.notifyObservers(message);
                Platform.runLater(() -> showAlert("Thành công", "Xóa vật phẩm thành công!"));
                break;

            case "DELETE_ITEM_FAILED":
                session.notifyObservers(message);
                Platform.runLater(() -> showAlert("Lỗi", "Xóa vật phẩm thất bại!"));
                break;

            case "DEPOSIT_SUCCESS":
                handleDepositSuccess(parts);
                break;

            case "ERROR":
                System.err.println("Lỗi  server: " + message);
                break;

            default:
                System.out.println("Unknown command: " + command);
        }
    }

    private void handleUserDetail(String[] parts, String message) {
        if (parts.length >= 6) {
            try {
                session.setCurrentFullname(parts[4].replace("_", " "));
                session.setCurrentBalance(Double.parseDouble(parts[5]));
                session.notifyObservers("USER_DATA_CHANGED");
            } catch (Exception e) {
                System.err.println("L?i c?p nh?t s? du t? USER_DETAIL: " + e.getMessage());
            }
        }
        session.notifyObservers(message);
    }

    private void handleLoginSuccess(String[] parts) {
        if (parts.length >= 5) {
            session.setCurrentRole(parts[1]);
            session.setCurrentFullname(parts[2].replace("_", " "));
            session.setCurrentBalance(Double.parseDouble(parts[3]));
            session.setCurrentUsername(parts[4]);

            session.notifyObservers("USER_DATA_CHANGED");

            Platform.runLater(() -> MainFx.showHomeByRole(session.getCurrentRole()));
        }
    }

    private void handleLogoutSuccess() {
        session.reset();
        Platform.runLater(() -> {
            try {
                MainFx.showLoginScene();
            } catch (Exception e) {
                System.err.println("L?i khi chuy?n v? m�n h�nh dang nh?p: " + e.getMessage());
            }
        });
    }

    private void handleBidFailed(String message) {
        String reason = message.replace("BID_FAILED ", "").replace("_", " ");
        Platform.runLater(() -> showAlert("�?u gi� th?t b?i", reason));
    }

    private void handleDepositSuccess(String[] parts) {
        if (parts.length >= 2) {
            session.setCurrentBalance(Double.parseDouble(parts[1]));
            session.notifyObservers("USER_DATA_CHANGED");
            Platform.runLater(() -> showAlert("Th�nh c�ng", "N?p ti?n th�nh c�ng! S? du m?i: " + session.getCurrentBalance() + "$"));
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
