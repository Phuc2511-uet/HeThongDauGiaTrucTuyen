package Controllers.controller;

import Controllers.NetWork.Client;
import Model.Observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AdminListUserController implements Observer {

    @FXML
    private VBox userContainer;

    @FXML
    public void initialize() {

        Client.getInstance().removeObserver(this);
        Client.getInstance().addObserver(this);

        loadUsers();
    }

    private void loadUsers() {

        userContainer.getChildren().clear();

        Client.getInstance().getUserIds();
    }

    @Override
    public void update(String message) {

        Platform.runLater(() -> {

            if (message.startsWith("USER_IDS")) {

                userContainer.getChildren().clear();

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

            else if (message.startsWith("USER_DETAIL")) {

                try {

                    String[] parts = message.split("\\s+");

                    int id = Integer.parseInt(parts[1]);

                    String username = parts[2];

                    String role = parts[3];

                    String fullname = parts[4].replace("_", " ");

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