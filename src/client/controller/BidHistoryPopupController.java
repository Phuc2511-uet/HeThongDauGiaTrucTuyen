package client.controller;

import client.network.ClientConnection;
import client.state.Observer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BidHistoryPopupController implements Observer {
    @FXML
    private TableView<String[]> tableHistory;
    @FXML
    private TableColumn<String[], String> colTime;
    @FXML
    private TableColumn<String[], String> colBidder;
    @FXML
    private TableColumn<String[], String> colPrice;

    @FXML
    private Label lblBidHistoryLink;

    private final ObservableList<String[]> historyData = FXCollections.observableArrayList();
    private int currentAuctionId;

    @FXML
    public void initialize() {
        ClientConnection.getInstance().addObserver(this);

        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[0]));
        colBidder.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1]));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[2]));

        tableHistory.setItems(historyData);
    }

    public void setAuctionId(int auctionId) {
        this.currentAuctionId = auctionId;
        ClientConnection.getInstance().send("GET_BID_HISTORY " + auctionId);
    }

    @Override
    public void update(String message) {

        String cleanMessage = message.trim();

        // ===== LOAD HISTORY =====
        if (cleanMessage.startsWith("BID_HISTORY")) {

            String[] parts = cleanMessage.split("\\s+");

            int resId = Integer.parseInt(parts[1]);

            if (resId == currentAuctionId) {

                Platform.runLater(() -> {

                    historyData.clear();

                    // FORMAT:
                    // BID_HISTORY auctionId time price username

                    for (int i = 2; i + 2 < parts.length; i += 3) {

                        String timeStr =
                                formatTime(Long.parseLong(parts[i]));

                        String priceStr =
                                String.format("%,.0f",
                                                Double.parseDouble(parts[i + 1]))
                                        .replace(",", " ");

                        String bidderStr =
                                parts[i + 2];

                        historyData.add(0,
                                new String[]{
                                        timeStr,
                                        bidderStr,
                                        priceStr
                                });
                    }
                });
            }
        }

        // ===== REALTIME UPDATE =====
        else if (cleanMessage.startsWith("NOTIFY")
                || cleanMessage.startsWith("AUTO_BID")) {

            String[] parts = cleanMessage.split("\\s+");

            if (parts.length >= 3) {

                try {

                    int actId = Integer.parseInt(parts[1]);

                    if (actId == currentAuctionId
                            && !cleanMessage.contains("SUCCESS")
                            && !cleanMessage.contains("FAILED")) {

                        Platform.runLater(() -> {

                            String timeStr =
                                    formatTime(System.currentTimeMillis());

                            String priceStr =
                                    String.format("%,.0f",
                                                    Double.parseDouble(parts[2]))
                                            .replace(",", " ");

                            String bidderStr =
                                    parts.length > 3
                                            ? parts[3]
                                            : "Hệ thống";

                            historyData.add(0,
                                    new String[]{
                                            timeStr,
                                            bidderStr,
                                            priceStr
                                    });
                        });
                    }

                } catch (Exception ignored) {
                }
            }
        }
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm:ss dd/MM").format(new Date(timestamp));
    }

    public void closePopup() {
        ClientConnection.getInstance().removeObserver(this);
    }
}
