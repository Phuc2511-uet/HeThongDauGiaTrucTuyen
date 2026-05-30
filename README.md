# Hệ thống Đấu giá Trực tuyến

Hệ thống Đấu giá Trực tuyến là ứng dụng client-server viết bằng Java, hỗ trợ người dùng tạo sản phẩm, mở phiên đấu giá, đặt giá, tự động đấu giá và thanh toán sau khi thắng phiên. Ứng dụng sử dụng JavaFX cho giao diện client, TCP Socket cho giao tiếp mạng và MySQL trên Aiven Cloud để lưu trữ dữ liệu.

## 1. Mô tả bài toán và phạm vi hệ thống

### Bài toán

Đấu giá trực tuyến cần xử lý đồng thời nhiều người dùng, cập nhật trạng thái phiên đấu giá kịp thời và đảm bảo dữ liệu tài khoản không bị sai lệch khi có nhiều lượt đặt giá. Hệ thống tập trung giải quyết các yêu cầu chính sau:

- Cập nhật giá hiện tại, trạng thái phiên và thông báo cho các client đang theo dõi.
- Xử lý các lượt đặt giá đồng thời ở phía server.
- Hỗ trợ đấu giá tự động dựa trên bước giá và mức giá tối đa do người dùng thiết lập.
- Quản lý số dư tài khoản khi đặt giá, bị vượt giá và thanh toán phiên đấu giá.
- Lưu trữ và khôi phục dữ liệu phiên đấu giá từ cơ sở dữ liệu.

### Phạm vi hệ thống

Hệ thống được xây dựng theo mô hình client-server:

- **Client:** giao diện JavaFX cho người mua, người bán và quản trị viên.
- **Server:** xử lý kết nối TCP Socket, nghiệp vụ đấu giá, quản lý người dùng, sản phẩm, phiên đấu giá và đồng bộ dữ liệu với database.
- **Database:** MySQL trên Aiven Cloud, truy cập thông qua JDBC.

Các nhóm người dùng chính:

- **Bidder:** đăng ký, đăng nhập, nạp tiền, xem phiên đấu giá, đặt giá, cấu hình auto-bid, theo dõi lịch sử và thanh toán phiên đã thắng.
- **Seller:** quản lý sản phẩm, tạo phiên đấu giá, theo dõi phiên do mình tạo và nhận tiền sau khi người thắng thanh toán.
- **Admin:** quản lý người dùng, sản phẩm và phiên đấu giá trong hệ thống.

## 2. Công nghệ sử dụng và yêu cầu cài đặt

### Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ chính | Java 17 |
| Giao diện | JavaFX 17.0.2, FXML, CSS |
| Giao tiếp mạng | TCP Socket |
| Cơ sở dữ liệu | MySQL trên Aiven Cloud |
| Truy cập database | JDBC, mysql-connector-j 8.3.0 |
| Kiểm thử | JUnit 5 / JUnit Jupiter 5.10.2 |
| Quản lý project | Apache Maven |

### Yêu cầu cài đặt

Trước khi chạy chương trình, cần cài đặt:

1. **JDK 17 trở lên**

   Kiểm tra bằng lệnh:

   ```bash
   java -version
   javac -version
   ```

2. **Apache Maven 3.6 trở lên**

   Kiểm tra bằng lệnh:

   ```bash
   mvn -version
   ```

3. **Kết nối Internet**

   Server cần kết nối Internet để truy cập MySQL trên Aiven Cloud.

## 3. Cấu trúc thư mục

Dự án được tổ chức theo các module chính sau:

```text
HeThongDauGiaTrucTuyen/
├── .github/                   # Cấu hình GitHub Actions / Workflows
├── src/
│   ├── client/                # Module client JavaFX
│   │   ├── MainFx.java        # Entry point của ứng dụng client
│   │   ├── controller/        # Controller xử lý sự kiện giao diện
│   │   ├── network/           # Kết nối TCP Socket tới server
│   │   ├── state/             # Quản lý trạng thái và cập nhật realtime ở client
│   │   └── view/resources/    # FXML, CSS và tài nguyên giao diện
│   │
│   ├── server/                # Module server
│   │   ├── MainServer.java    # Entry point của server
│   │   ├── network/           # Server socket và xử lý kết nối client
│   │   ├── repository/        # Kết nối database và quản lý cache
│   │   └── service/           # Xử lý nghiệp vụ hệ thống
│   │
│   ├── shared/                # Các lớp dùng chung giữa client và server
│   │   ├── exception/         # Exception tự định nghĩa
│   │   └── model/             # User, Item, Auction, AutoBid, BidTransaction...
│   │       └── item/factory/  # Factory khởi tạo các loại sản phẩm
│   │
│   └── test/                  # Unit test
│       └── AuctionTest.java
│
├── pom.xml                    # Cấu hình Maven
└── README.md
```

## 4. Một số design pattern đã áp dụng

- **Factory Method:** dùng trong `ItemFactory` để khởi tạo các loại sản phẩm như `Art`, `Electronic`, `Vehicle`.
- **State Machine:** quản lý vòng đời phiên đấu giá qua các trạng thái như `OPEN`, `RUNNING`, `FINISH`, `PAID`, `CANCELED`.
- **Observer:** hỗ trợ cập nhật thông tin phiên đấu giá từ server tới các client đang theo dõi.
- **Singleton:** dùng cho một số lớp quản lý kết nối và trạng thái dùng chung, ví dụ `ClientConnection`, `InformationHandle`, `UserManager`.

## 5. Hướng dẫn chạy chương trình

Các lệnh dưới đây có thể chạy trên Windows, Linux và macOS nếu đã cài JDK và Maven.

### 5.1. Biên dịch và đóng gói

Tại thư mục gốc của dự án, chạy:

```bash
mvn clean package
```

Sau khi build thành công, file JAR được tạo tại:

```text
target/HeThongDauGia-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 6. Thứ tự chạy Server và Client

Cần khởi động **Server trước**, sau đó mới mở **Client**.

### Bước 1: Chạy Server

Cách 1: chạy bằng Maven:

```bash
mvn exec:java -Dexec.mainClass="server.MainServer"
```

Cách 2: chạy bằng file JAR đã build:

```bash
java -cp target/HeThongDauGia-1.0-SNAPSHOT-jar-with-dependencies.jar server.MainServer
```

Khi server khởi động thành công, console sẽ hiển thị thông tin nạp dữ liệu người dùng, sản phẩm và phiên đấu giá từ database.

### Bước 2: Chạy Client

Mở một terminal khác, sau đó chạy một trong các lệnh sau.

Cách 1: chạy bằng Maven:

```bash
mvn exec:java -Dexec.mainClass="client.MainFx"
```

Cách 2: chạy bằng file JAR:

```bash
java -cp target/HeThongDauGia-1.0-SNAPSHOT-jar-with-dependencies.jar client.MainFx
```

> Nếu file JAR được cấu hình main class cho client trong `pom.xml`, có thể chạy bằng lệnh:
>
> ```bash
> java -jar target/HeThongDauGia-1.0-SNAPSHOT-jar-with-dependencies.jar
> ```

## 7. Cấu hình kết nối Client-Server

Mặc định, client kết nối tới địa chỉ IP được cấu hình trong `MainFx.java`:

```java
ClientConnection.getInstance().connect("100.97.45.37", 3636);
```

Nếu chạy server và client trên cùng một máy, đổi địa chỉ IP thành `127.0.0.1` hoặc `localhost`:

```java
ClientConnection.getInstance().connect("127.0.0.1", 3636);
```

Sau khi thay đổi cấu hình, build lại project:

```bash
mvn clean package
```

## 8. Chức năng đã hoàn thành

### 8.1. Lõi hệ thống

- [x] Giao tiếp client-server bằng TCP Socket.
- [x] Xử lý nhiều kết nối client ở phía server.
- [x] Gửi và lưu ảnh sản phẩm thông qua giao thức `UPLOAD_IMAGE`.
- [x] Đồng bộ dữ liệu giữa bộ nhớ server và MySQL Cloud Database.
- [x] Khôi phục dữ liệu phiên đấu giá khi server khởi động lại.
- [x] Unit test cho một số lớp model và luồng xử lý chính.

### 8.2. Bidder

- [x] Đăng ký và đăng nhập tài khoản.
- [x] Nạp tiền vào ví cá nhân.
- [x] Xem danh sách các phiên đấu giá đang mở hoặc đang chạy.
- [x] Xem chi tiết phiên đấu giá.
- [x] Đặt giá trực tiếp theo bước giá hợp lệ.
- [x] Cấu hình auto-bid với mức giá tối đa.
- [x] Xem biểu đồ biến động giá của phiên đấu giá.
- [x] Nhận thông báo khi có thay đổi liên quan đến phiên đấu giá.
- [x] Xem các phiên đã thắng và thanh toán bằng số dư tài khoản.

### 8.3. Seller

- [x] Thêm sản phẩm mới.
- [x] Chọn danh mục sản phẩm: tranh ảnh, điện tử, xe cộ.
- [x] Cập nhật hoặc xóa sản phẩm chưa đưa vào đấu giá.
- [x] Tạo phiên đấu giá cho sản phẩm hợp lệ.
- [x] Xem danh sách và trạng thái các phiên đấu giá do mình tạo.
- [x] Nhận tiền sau khi người thắng thanh toán phiên đấu giá.

### 8.4. Admin

- [x] Đăng nhập bằng tài khoản quản trị viên.
- [x] Xem và xóa tài khoản người dùng.
- [x] Xem và xóa sản phẩm.
- [x] Tạo tài khoản mới từ giao diện Admin.
- [x] Hủy phiên đấu giá đang chạy khi cần.
- [x] Khôi phục phiên đấu giá đã hủy về trạng thái chờ đấu giá và đặt lại thời gian.

## 9. Báo cáo và video demo

- **Báo cáo PDF:** Cập nhật sau.
- **Video demo:** Cập nhật sau.
