package Controllers.Controller;

import View.Client.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AdminListUserController implements Observer {

    @FXML
    private VBox userContainer;

    private final java.util.Set<Integer> loadedUserIds = new java.util.HashSet<>();

    @FXML
    public void initialize() {

        // 'removeIf' để dọn sạch hoàn toàn các instance cũ thuộc kiểu AdminListUserController
        // -> tránh lỗi trùng lặp/nhân đôi sự kiện
        Client.getInstance().getObservers().removeIf(obs -> obs instanceof AdminListUserController);
        Client.getInstance().addObserver(this);

        loadUsers();
    }

    private void loadUsers() {
        loadedUserIds.clear();
        userContainer.getChildren().clear();
        Client.getInstance().getUserIds();
    }

    @Override
    public void update(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("USER_IDS")) {
                userContainer.getChildren().clear();
                loadedUserIds.clear();
                String[] parts = message.split("\\s+");
                for (int i = 1; i < parts.length; i++) {
                    try {
                        int userId = Integer.parseInt(parts[i]);
                        Client.getInstance().getUserById(userId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            else if (message.startsWith("ADMIN_USER_DETAIL")) {
                try {
                    String[] parts = message.split("\\s+");
                    int id = Integer.parseInt(parts[1]);
                    String username = parts[2];
                    String role = parts[3];
                    String fullname = parts[4].replace("_", " ");
                    if (role.equalsIgnoreCase("ADMIN")) {
                        return;
                    }
                    if (loadedUserIds.contains(id)) {
                        return;
                    }
                    loadedUserIds.add(id);
                    HBox card = createUserCard(
                            id,
                            username,
                            fullname,
                            role
                    );
                    userContainer.getChildren().add(card);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            else if (message.startsWith("DELETE_USER_SUCCESS")) {
                loadUsers();
            }
        });
    }

    @FXML
    private void handleAdminCreateAccount() {
        HomeAdminController.setPage("/View/resources/fxml/adminCreateAccount.fxml");
    }

    private HBox createUserCard(
            int id,
            String username,
            String fullname,
            String role
    ) {

        HBox box = new HBox();
        box.setSpacing(20);
        box.setPadding(new Insets(15));
        box.setStyle(
                "-fx-background-color:#1E293B;" +
                        "-fx-background-radius:15;"
        );

        Label info = new Label(
                "ID: " + id +
                        " | Username: " + username +
                        " | Fullname: " + fullname +
                        " | Role: " + role
        );

        info.setStyle(
                "-fx-text-fill:white;" +
                        "-fx-font-size:15;" +
                        "-fx-font-weight:bold;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button deleteBtn = new Button("Xóa");

        deleteBtn.setStyle(
                "-fx-background-color:#DC2626;" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:10;"
        );

        deleteBtn.setOnAction(e -> {
            Client.getInstance().deleteUser(id);
        });

        box.getChildren().addAll(
                info,
                spacer,
                deleteBtn
        );

        return box;
    }
}