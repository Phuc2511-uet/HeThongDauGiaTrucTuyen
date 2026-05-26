package Controllers.Controller;

import View.Client.Client;
import Model.Observer.Observer;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class PriceChartPopupController implements Observer {

    @FXML private LineChart<String, Number> priceLineChart;
    @FXML private CategoryAxis xAxisTime;
    @FXML private NumberAxis yAxisPrice;

    private XYChart.Series<String, Number> priceSeries;
    private int currentAuctionId;

    @FXML
    public void initialize() {
        priceSeries = new XYChart.Series<>();
        priceLineChart.getData().add(priceSeries);

        // Đường nối Line cam vàng dày dặn
        priceSeries.getNode().setStyle("-fx-stroke: #F59E0B; -fx-stroke-width: 3px;");

        // Ép đồ thị bám sát biên trái và phải
        xAxisTime.setGapStartAndEnd(false);

        // Hiện lưới ngang nét đứt, ẩn lưới dọc
        priceLineChart.setHorizontalGridLinesVisible(true);
        priceLineChart.setVerticalGridLinesVisible(false);

        Client.getInstance().addObserver(this);
    }

    public void setAuctionId(int auctionId) {
        this.currentAuctionId = auctionId;
        Client.getInstance().send("GET_BID_HISTORY " + auctionId);
    }

    @Override
    public void update(String message) {
        String cleanMessage = message.trim();

        // ===== 1. NẠP TOÀN BỘ LỊCH SỬ PHIÊN =====
        if (cleanMessage.startsWith("BID_HISTORY")) {
            String[] parts = cleanMessage.split("\\s+");
            int resId = Integer.parseInt(parts[1]);

            if (resId == currentAuctionId) {
                Platform.runLater(() -> {
                    priceSeries.getData().clear();

                    for (int i = 2; i + 2 < parts.length; i += 3) {
                        try {
                            String timeStr = formatTime(Long.parseLong(parts[i]));
                            double price = Double.parseDouble(parts[i + 1]);

                            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(timeStr, price);
                            priceSeries.getData().add(dataPoint);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        // ===== 2. CẬP NHẬT REALTIME BIẾN ĐỘNG GIÁ =====
        else if (cleanMessage.startsWith("NOTIFY") || cleanMessage.startsWith("AUTO_BID")) {
            String[] parts = cleanMessage.split("\\s+");
            if (parts.length >= 3) {
                try {
                    int actId = Integer.parseInt(parts[1]);
                    if (actId == currentAuctionId && !cleanMessage.contains("SUCCESS") && !cleanMessage.contains("FAILED")) {
                        Platform.runLater(() -> {
                            String timeStr = formatTime(System.currentTimeMillis());
                            double price = Double.parseDouble(parts[2]);

                            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(timeStr, price);
                            priceSeries.getData().add(dataPoint);
                        });
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm:ss\ndd/MM").format(new Date(timestamp));
    }

    public void closePopup() {
        Client.getInstance().removeObserver(this);
    }
}
