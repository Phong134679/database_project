# Digital Library Management System & Open Repository

Hệ thống Quản lý Thư viện Số và Kho Lưu trữ Mở — ứng dụng web full-stack sử dụng **Java 11+ (JDK HttpServer + JDBC thuần + Gson)**, **MySQL 8.0+** và giao diện **HTML/CSS/Vanilla JavaScript** (Single Page Application).

---

## 1. Tính năng chính

- **Danh mục Tài nguyên (`RESOURCE`)**: Quản lý sách in (`PHYSICAL_BOOK`), tạp chí số (`DIGITAL_JOURNAL`), và các tài nguyên có đồng thời cả hai định dạng; theo dõi số bản sao vật lý (`BOOK_COPY`) và số gói bản quyền yêu cầu (`RESOURCE_LICENSE`).
- **Mượn & Trả sách (`BORROW`)**:
  - Chỉ thành viên đại học (`UNIVERSITY_MEMBER`) được mượn sách về nhà:
    - **Sinh viên (`STUDENT`)**: Tối đa **3** cuốn, thời hạn **14 ngày**.
    - **Giảng viên (`FACULTY`)**: Tối đa **10** cuốn, thời hạn **30 ngày**.
  - Tự động chặn mượn nếu thành viên đang giữ sách quá hạn, có phiếu phạt chưa thanh toán, đạt giới hạn mượn, bản sao không sẵn sàng, hoặc thiếu gói bản quyền (tác giả của sách được miễn kiểm tra bản quyền cho chính tài nguyên đó).
  - Trả sách trễ hạn tự động phát sinh hoặc cộng dồn phiếu phạt **50,000 VND** (`Unpaid`).
- **Quản lý Tiền phạt (`FINE` & `PAYMENT_TRANSACTION`)**: Thủ thư xác nhận thanh toán phiếu phạt (`Unpaid` → `Completed`) và lưu lịch sử giao dịch.
- **Truy cập Tạp chí Số & Bản quyền (`DIGITAL_JOURNAL`, `LICENSE_PACKAGE`, `GRANTED_LICENSE`, `ACCESS_LOG`)**:
  - **Guest** (không định danh): Chỉ truy cập được tạp chí công khai (`is_public = TRUE`).
  - **Người dùng định danh (`PERSON`)**: Được truy cập nếu là tác giả (`AUTHOR_OF`) hoặc sở hữu đầy đủ các gói bản quyền bắt buộc (`GRANTED_LICENSE`).
  - Mọi lượt truy cập hợp lệ đều được ghi nhận vào `ACCESS_LOG`.
  - Hỗ trợ cấp và thu hồi quyền bản quyền thủ công cho từng `PERSON`.

---

## 2. Cấu trúc thư mục

```text
database_project/
├── Project/
│   ├── src/
│   │   ├── db.properties                         # Cấu hình kết nối MySQL (không commit mật khẩu thật)
│   │   └── com/library/
│   │       ├── Main.java                         # Kiểm tra kết nối MySQL Server & database library_db
│   │       ├── api/                              # ApiServer (port 8080), HttpUtils, JsonUtils
│   │       ├── service/                          # Logic nghiệp vụ & điều phối JDBC transaction
│   │       ├── repository/                       # Truy vấn JDBC (PreparedStatement) trên 14 bảng MySQL
│   │       ├── model/                            # 10 lớp Entity POJO
│   │       ├── validation/                       # Kiểm tra dữ liệu đầu vào & tồn tại thực thể
│   │       ├── exception/                        # Hệ thống ngoại lệ chuẩn hóa (errorCode + HTTP status)
│   │       └── util/                             # DatabaseConnection, DatabaseInitializer, TransactionManager
│   ├── web/                                      # Giao diện Web SPA (index.html, style.css, app.js)
│   ├── lib/                                      # Thư viện: mysql-connector-j, gson
│   ├── 01_create_database_final_v2.sql           # Script tạo database library_db và 14 bảng
│   ├── 02_insert_data_final_v2.sql               # Script nạp dữ liệu mẫu
│   └── start_server.sh                           # Script biên dịch & khởi chạy server trên cổng 8080
├── SQL_Reference/                                # Bộ SQL tham khảo độc lập (Views/Functions/Procedures)
└── README.md
```

---

## 3. Yêu cầu môi trường

- **Java Development Kit (JDK)**: Phiên bản **11** trở lên (`java -version`, `javac -version`).
- **MySQL Server**: Phiên bản **8.0** trở lên đang chạy trên cổng `3306`.
- *(Không bắt buộc cài Maven, Gradle hay framework ngoài — thư viện JDBC và Gson đã nằm sẵn trong `Project/lib/`).*

---

## 4. Hướng dẫn cài đặt & Chạy nhanh

### Bước 1: Cấu hình kết nối MySQL
Chỉnh sửa file `Project/src/db.properties` với tài khoản MySQL trên máy của bạn:

```properties
db.url=jdbc:mysql://localhost:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
db.username=root
db.password=MAT_KHAU_MYSQL_CUA_BAN
db.driver=com.mysql.cj.jdbc.Driver
```

### Bước 2: Khởi tạo cơ sở dữ liệu `library_db`
Bạn có thể nạp tự động bằng `DatabaseInitializer` (sau khi đã biên dịch) hoặc chạy trực tiếp qua MySQL CLI:

```bash
mysql -u root -p < Project/01_create_database_final_v2.sql
mysql -u root -p < Project/02_insert_data_final_v2.sql
```

Hoặc dùng tiện ích Java có sẵn:

```bash
cd Project
mkdir -p bin
javac -encoding UTF-8 -d bin -cp "lib/*" $(find src -name "*.java")
cp src/db.properties bin/db.properties
java -cp "bin:lib/*" com.library.util.DatabaseInitializer
```

### Bước 3: Khởi động ứng dụng
Chạy script khởi động (Linux/macOS):

```bash
cd Project
./start_server.sh
```

Hoặc chạy thủ công:

```bash
cd Project
java -cp "bin:lib/*" com.library.api.ApiServer 8080
```

Mở trình duyệt tại: **http://localhost:8080/**

---

## 5. Kiến trúc & Danh sách API

Ứng dụng tuân theo kiến trúc phân tầng **API → Service → Repository → MySQL**:

- **API Layer (`com.library.api`)**: Tiếp nhận HTTP request, parse/serialize JSON và ánh xạ ngoại lệ sang HTTP status code (`200`, `201`, `400`, `403`, `404`, `500`).
- **Service Layer (`com.library.service`)**: Chứa toàn bộ logic nghiệp vụ và quản lý transaction qua `TransactionManager` (`setAutoCommit(false)`, `commit()`, `rollback()`).
- **Repository Layer (`com.library.repository`)**: Thực thi câu lệnh SQL qua `PreparedStatement` trực tiếp trên 14 bảng MySQL, sử dụng `SELECT ... FOR UPDATE` tại các điểm cần khóa đồng thời.

| Method | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Kiểm tra trạng thái hoạt động của API Server |
| `GET` | `/api/resources` | Lấy danh mục tài nguyên (sách in, tạp chí số, bản sao, license) |
| `GET` | `/api/copies` | Lấy danh sách toàn bộ bản sao sách vật lý |
| `GET` | `/api/members` | Lấy danh sách người dùng, trạng thái thành viên, hạn mức và điều kiện mượn |
| `GET` | `/api/borrows` | Lấy danh sách lượt mượn và trạng thái (`Active`, `Overdue`, `Returned`) |
| `POST` | `/api/borrows` | Tạo lượt mượn mới (`{ "memberId", "copyId" }`) |
| `POST` | `/api/borrows/{id}/return` | Trả sách (`{ "returnDate"? }`) và tự động tính phạt nếu trễ hạn |
| `GET` | `/api/fines` | Lấy danh sách phiếu phạt |
| `GET` | `/api/fines/{id}` | Xem chi tiết một phiếu phạt |
| `POST` | `/api/fines/{id}/payments` | Xác nhận đã thanh toán phiếu phạt |
| `GET` | `/api/digital-journals` | Lấy danh sách tạp chí số |
| `POST` | `/api/digital-journals/{id}/access` | Thẩm định quyền đọc tạp chí số (`{ "personId"? }`) và ghi `ACCESS_LOG` |
| `GET` | `/api/access-logs` | Lấy nhật ký truy cập tạp chí số |
| `GET` | `/api/licenses` | Lấy danh sách các gói bản quyền (`LICENSE_PACKAGE`) |
| `GET` | `/api/granted-licenses` | Lấy danh sách cấp phép bản quyền hiện có |
| `POST` | `/api/granted-licenses` | Cấp gói bản quyền thủ công (`{ "personId", "licenseId" }`) |
| `DELETE` | `/api/granted-licenses/{id}` | Thu hồi quyền bản quyền theo `grantId` |

---

## 6. Tài liệu SQL Reference (Tùy chọn)

Thư mục [`SQL_Reference/`](SQL_Reference/README.md) cung cấp bộ Views, Stored Functions và Stored Procedures tham khảo độc lập (triển khai trên database thử nghiệm riêng `library_management_reference`) để đối chiếu với cách cài đặt Java + JDBC hiện tại. Thành phần này không tham gia vào quá trình chạy của ứng dụng chính.
