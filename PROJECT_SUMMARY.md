# BÁO CÁO TỔNG KẾT DỰ ÁN & VAI TRÒ CỦA FILE `db.properties`
> **Dự án**: Digital Library Management System & Open Repository  
> **Khóa học**: Hệ quản trị Cơ sở Dữ liệu (Database Management Systems) - Nhóm G5  
> **Công nghệ**: Java 11+ (JDK 25), MySQL 8.0+, JDBC Driver (Connector/J 26.7.0), MVC + Repository Pattern.

---

## PHẦN 1: TỔNG HỢP TOÀN BỘ CÔNG VIỆC ĐÃ HOÀN THÀNH

Hệ thống đã được xây dựng, hoàn thiện và kiểm thử thực tế 100% từ mô hình CSDL, tầng truy xuất dữ liệu, tầng nghiệp vụ đến kịch bản kiểm thử trực tiếp trên MySQL Server:

```
database_project/
├── .gitignore                                      # Cấu hình chặn file rác, metadata và file nhạy cảm
├── README.md                                       # Tài liệu hướng dẫn cài đặt & kiến trúc hệ thống
├── STEP1_SUMMARY.md                                # Báo cáo chi tiết nghiệp vụ Bước 1 (Phạt & Thanh toán)
├── STEP2_SUMMARY.md                                # Báo cáo chi tiết nghiệp vụ Bước 2 (Phân quyền & Bản quyền)
├── PROJECT_SUMMARY.md                              # Báo cáo tổng hợp toàn diện & giải thích db.properties
├── Source/                                         # Tài liệu đặc tả và thiết kế hệ thống
│   ├── G5_phase1.pdf                               # Báo cáo đặc tả yêu cầu giai đoạn 1 (SRS)
│   ├── erDiagram.md                                # Bản vẽ mô hình thực thể quan hệ Mermaid ERD
│   └── 808050965_2457431158118432_...png           # Sơ đồ ERD trực quan
└── Project/                                        # Mã nguồn dự án Java (Eclipse Project)
    ├── .project / .classpath / .settings           # Cấu hình project chuẩn hóa UTF-8 & compiler
    ├── schema.sql                                  # Kịch bản tạo CSDL library_db và 15 bảng InnoDB
    ├── sample_data.sql                             # Dữ liệu mẫu kiểm thử đầy đủ các thực thể
    ├── lib/
    │   └── mysql-connector-j-26.7.0.jar            # Thư viện JDBC Driver kết nối MySQL
    └── src/
        ├── db.properties                           # Cấu hình kết nối MySQL cục bộ (đã điền mật khẩu)
        ├── db.properties.example                   # Template mẫu cấu hình kết nối an toàn cho Git
        └── com/library/
            ├── Main.java                           # Entry point chạy thử trực tiếp 4 Validation Cases
            ├── util/
            │   ├── DatabaseConnection.java         # Quản lý Connection JDBC qua db.properties
            │   └── DatabaseInitializer.java        # [Mới] Tự động tạo CSDL, schema & nạp sample data
            ├── controller/
            │   └── LibraryController.java          # Điều phối Mượn, Trả, Phạt, Bản quyền, Nhật ký
            ├── service/                            # Tầng xử lý nghiệp vụ & logic kiểm tra ràng buộc
            │   ├── BorrowService.java              # Nghiệp vụ mượn, trả sách & tính phạt trễ hạn
            │   ├── FineService.java                # [Mới] Xử lý thanh toán trả góp tiền phạt (BR11)
            │   ├── MemberService.java              # [Mới] Phân quyền & kiểm tra tư cách mượn (BR01, BR05)
            │   └── DigitalJournalService.java      # [Mới] Quản lý bản quyền số & ghi log (BR04, BR13)
            ├── repository/                         # Tầng truy xuất CSDL trực tiếp qua PreparedStatement
            │   ├── PersonRepository.java           # [Mới] CRUD bảng PERSON
            │   ├── UniversityMemberRepository.java # [Mới] CRUD bảng UNIVERSITY_MEMBER (JOIN PERSON)
            │   ├── DigitalJournalRepository.java   # [Mới] CRUD bảng DIGITAL_JOURNAL (JOIN RESOURCE)
            │   ├── LicensePackageRepository.java   # [Mới] Quản lý LICENSE_PACKAGE & RESOURCE_LICENSE
            │   ├── GrantedLicenseRepository.java   # [Mới] Quản lý GRANTED_LICENSE & kiểm tra thời hạn
            │   ├── AccessLogRepository.java        # [Mới] Ghi nhật ký truy cập tốc độ cao (BR13)
            │   ├── BorrowRepository.java           # Thao tác BORROW, kiểm tra hạn mức & sách trễ hạn
            │   ├── FineRepository.java             # [Mới] Thao tác FINE, kiểm tra nợ phạt chưa đóng
            │   └── PaymentTransactionRepository.java # [Mới] Thao tác PAYMENT_TRANSACTION & tính tổng nộp
            └── model/                              # 15 POJOs ánh xạ 1:1 với 15 bảng CSDL
```

### 1.1. Hiện thực Bước 1: Phân hệ Phạt vi phạm & Thanh toán trả góp
* **Xây dựng mới**: `FineRepository.java`, `PaymentTransactionRepository.java`, `FineService.java`.
* **Nghiệp vụ Test 03 (FR05 & BR02)**: Khi độc giả trả sách quá hạn `due_date`, hệ thống tự động tính số ngày trễ nhân đơn giá (10.000 VND / ngày) và phát sinh một bản ghi mới trong bảng `FINE` với trạng thái `Unpaid` và hạn nộp 14 ngày.
* **Nghiệp vụ Test 04 (FR06 & BR11)**: Hỗ trợ nộp phạt trả góp nhiều đợt. Mỗi đợt lưu vào `PAYMENT_TRANSACTION`, hệ thống tự động tính lũy kế và chuyển trạng thái: `Unpaid` $\rightarrow$ `Partially Paid` $\rightarrow$ `Completed`.

### 1.2. Hiện thực Bước 2: Phân quyền Độc giả & Quản lý Bản quyền số
* **Xây dựng mới**: `PersonRepository.java`, `UniversityMemberRepository.java`, `DigitalJournalRepository.java`, `LicensePackageRepository.java`, `GrantedLicenseRepository.java`, `AccessLogRepository.java`, `MemberService.java`, `DigitalJournalService.java`.
* **Nghiệp vụ Test 01 (BR01 & BR05)**:
  * Chặn khách ngoài (`GUEST`, `EXTERNAL_AUTHOR`) mượn sách về nhà (chỉ được đọc tại chỗ).
  * Chặn mượn sách mới nếu độc giả đang giữ sách trễ hạn chưa trả hoặc có tiền phạt chưa nộp.
  * Phân hóa hạn mức và thời gian mượn: Sinh viên tối đa 3 cuốn (14 ngày); Giảng viên tối đa 10 cuốn (30 ngày).
* **Nghiệp vụ Test 02 (BR04, BR12, BR13, FR07, FR08)**:
  * Mặc định khóa tài liệu số; chỉ mở khi độc giả có `GRANTED_LICENSE` còn hiệu lực (`valid_from <= date <= valid_to`). Nếu hết hạn $\rightarrow$ Chặn quyền đọc.
  * Tự động ghi nhận thông tin truy cập vào `ACCESS_LOG` (ghi nhận cả thành công lẫn bị chặn). Tách rời Foreign Key để tối ưu tốc độ ghi với tần suất cao (BR13).

### 1.3. Chuẩn hóa Cấu hình Dự án & Dọn dẹp File Rác
* **Sửa lỗi đường dẫn tuyệt đối trong `.classpath`**: Thay thế đường dẫn máy cục bộ cũ `path="/home/yuzu/..."` bằng đường dẫn tương đối chuẩn `path="lib/mysql-connector-j-26.7.0.jar"`, đảm bảo mọi thành viên khi clone về đều mở được dự án ngay trong Eclipse mà không bị lỗi thiếu thư viện.
* **Dọn dẹp file rác**: Xóa thư mục rác `Project/.metadata/`, xóa các bản sao trùng lặp `Project/README.md`, `Project/STEP1_SUMMARY.md`, `Project/STEP2_SUMMARY.md`.
* **Khôi phục cấu trúc**: Đưa thư mục tài liệu `Source/` về đúng vị trí gốc của repository.

### 1.4. Khởi tạo Cơ sở Dữ liệu & Kiểm thử Kết nối MySQL Thực tế
* **Xây dựng `DatabaseInitializer.java`**: Tự động kết nối MySQL server qua JDBC, tạo CSDL `library_db` (UTF-8 MB4), thực thi [schema.sql](Project/schema.sql) tạo 15 bảng và thực thi [sample_data.sql](Project/sample_data.sql) nạp dữ liệu mẫu mà không cần cài đặt thêm công cụ dòng lệnh MySQL CLI.
* **Xử lý Parser SQL**: Tối ưu bộ đọc script để bóc tách inline comments (`--`), đảm bảo nạp dữ liệu chuẩn xác.
* **Kiểm thử thực tế**: Chạy thành công [Main.java](Project/src/com/library/Main.java) trên máy chủ MySQL thật, toàn bộ 4 Validation Cases đều được kiểm chứng và đạt kết quả mong đợi.

---

## PHẦN 2: VAI TRÒ VÀ TẦM QUAN TRỌNG CỦA FILE `db.properties`

Trong các ứng dụng doanh nghiệp và dự án phần mềm chuyên nghiệp sử dụng Java & CSDL, file `db.properties` đóng vai trò là **tấm lá chắn phân tách giữa Mã nguồn (Source Code) và Cấu hình Môi trường (Configuration)**.

### 2.1. File `db.properties` là gì?
`db.properties` là một file văn bản thuần túy (plain text) tuân theo định dạng chuẩn **Java Properties** (cặp khóa - giá trị `key=value`):

```properties
# Cấu hình kết nối MySQL Database
db.url=jdbc:mysql://localhost:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
db.username=root
db.password=MẬT_KHẨU_MYSQL_CỦA_BẠN
db.driver=com.mysql.cj.jdbc.Driver
```

Các tham số cấu hình bao gồm:
* `db.url`: Chuỗi JDBC Connection URL chỉ định máy chủ (`localhost`), cổng (`3306`), tên database (`library_db`), múi giờ (`serverTimezone=Asia/Ho_Chi_Minh`) và bộ mã hóa tiếng Việt (`characterEncoding=UTF-8`).
* `db.username`: Tên tài khoản quản trị CSDL (mặc định là `root`).
* `db.password`: Mật khẩu đăng nhập CSDL tương ứng trên máy tính của bạn.
* `db.driver`: Lớp điều khiển JDBC của MySQL (`com.mysql.cj.jdbc.Driver`).

---

### 2.2. Vai trò Cốt lõi của `db.properties` trong Dự án

#### 1. Nguyên lý Tách rời Mã nguồn và Cấu hình (Decoupling Code & Configuration)
* **Nguyên tắc thiết kế (12-Factor App Methodology)**: Thông tin kết nối môi trường tuyệt đối **không được gán cứng (hardcode)** trực tiếp bên trong code Java.
* **Lợi ích**:
  * Nếu đổi mật khẩu MySQL, đổi cổng kết nối (ví dụ từ `3306` sang `3307`), hoặc chuyển CSDL sang một máy chủ khác trong mạng LAN, lập trình viên **chỉ cần sửa 1 dòng trong file `db.properties`**.
  * **Không cần phải sửa code Java và không cần phải biên dịch lại toàn bộ dự án** (`recompile`).

#### 2. Phục vụ Làm việc Nhóm Hiệu quả (Team Collaboration)
* Trong một nhóm sinh viên hoặc nhóm phát triển:
  * Máy tính của Bạn A dùng mật khẩu root là `Tonyphong432`.
  * Máy tính của Bạn B dùng XAMPP với mật khẩu root rỗng `""`.
  * Máy tính của Bạn C cài MySQL trên Docker hoặc cổng khác.
* **Nhờ có `db.properties`**: Mỗi thành viên tự cấu hình file trên máy mình để chạy mã nguồn mà không làm ảnh hưởng hay gây xung đột mã nguồn (git conflict) với các thành viên khác.

#### 3. Bảo vệ An toàn Thông tin & Bảo mật (Security)
* Thông tin mật khẩu CSDL là dữ liệu nhạy cảm. Nếu viết mật khẩu trực tiếp trong `DatabaseConnection.java`, khi đẩy code lên GitHub công khai, mật khẩu sẽ bị lộ cho toàn bộ cộng đồng mạng.
* Bằng cách đặt mật khẩu trong `db.properties` và cấu hình [`.gitignore`](.gitignore) để bỏ qua file này, mật khẩu cá nhân sẽ được lưu an toàn trên máy cục bộ của bạn mà không bao giờ bị lộ lên Internet.

#### 4. Phối hợp với File Mẫu `db.properties.example`
* Để các thành viên mới khi clone dự án về biết cách cấu hình, dự án cung cấp sẵn file template [Project/src/db.properties.example](Project/src/db.properties.example).
* Quy trình chuẩn:
  1. Thành viên mới clone mã nguồn từ GitHub về.
  2. Tạo bản sao file `db.properties.example` và đổi tên thành `db.properties`.
  3. Điền mật khẩu MySQL của máy mình vào file vừa tạo và chạy dự án.

---

### 2.3. Cơ chế Hoạt động của `db.properties` trong Mã nguồn

Trong lớp [DatabaseConnection.java](Project/src/com/library/util/DatabaseConnection.java), hệ thống sử dụng `ClassLoader` để tự động dò tìm và nạp file cấu hình khi chương trình khởi động:

```java
Properties props = new Properties();
InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties");
if (in != null) {
    props.load(in);
    url = props.getProperty("db.url", url);
    username = props.getProperty("db.username", username);
    password = props.getProperty("db.password", password);
    driver = props.getProperty("db.driver", driver);
    in.close();
}
```

* **Lưu ý kỹ thuật quan trọng**: Trong môi trường Eclipse hoặc khi biên dịch qua dòng lệnh, file `db.properties` nằm trong thư mục `src/` sẽ được ClassLoader tìm kiếm ở thư mục gốc của Classpath (tương đương với `bin/`). Do đó, file luôn cần được đồng bộ vào thư mục build để ứng dụng đọc được thông số mới nhất.

---

### 2.4. Khuyến nghị Bảo mật trước khi Commit lên Git

Để đảm bảo mật khẩu cá nhân của bạn không bị đẩy lên GitHub, hãy thực hiện lệnh sau trên terminal của dự án:

```bash
# Hướng dẫn Git bỏ qua mọi thay đổi đối với db.properties trên máy cục bộ
git update-index --assume-unchanged Project/src/db.properties
```

Lệnh này giúp bạn thoải mái lưu mật khẩu trong `db.properties` để chạy và kiểm thử code trên máy mình mà Git vẫn coi file này là nguyên bản, không bao giờ tự ý commit mật khẩu lên GitHub.
