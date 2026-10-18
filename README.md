# Digital Library Management System & Open Repository
> Hệ thống Quản lý Thư viện số và Lưu trữ mở — Dự án Cơ sở dữ liệu & Ứng dụng Java tương tác MySQL.

Dự án được xây dựng theo mô hình kiến trúc **MVC (Model - View - Controller)** kết hợp **Repository Pattern**, kết nối và xử lý dữ liệu trực tiếp với hệ quản trị cơ sở dữ liệu **MySQL**.

---

## 1. Hướng dẫn Setup & Cài đặt Môi trường (Setup Guide)

Mục này dành cho mọi thành viên trong nhóm khi clone mã nguồn về máy, giúp phân định rõ những gì **cần cài đặt thêm** và những gì **đã có sẵn trong dự án**, đảm bảo môi trường và thư viện hoạt động đồng nhất.

### 1.1. Những thành phần ĐÃ CÓ SẴN trong mã nguồn (Không cần tải thêm)
* **Thư viện MySQL JDBC Driver**:
  * File [`Project/lib/mysql-connector-j-26.7.0.jar`](Project/lib/mysql-connector-j-26.7.0.jar) đã được tích hợp sẵn trong thư mục `lib/` của dự án.
  * Đã được cấu hình liên kết sẵn trong file [`.classpath`](Project/.classpath) của Eclipse. Bạn **không cần** phải tải thêm file `.jar` từ bên ngoài.
* **Cấu hình dự án Eclipse**:
  * Các file metadata dự án [`.project`](Project/.project) và [`.classpath`](Project/.classpath) đã chuẩn hóa cấu trúc source folder (`src/`), output folder (`bin/`) và liên kết thư viện driver.
* **Script Cơ sở Dữ liệu**:
  * [`schema.sql`](schema.sql): Script tạo database `library_db` và định nghĩa 15 bảng chuẩn quan hệ.
  * [`sample_data.sql`](sample_data.sql): Script nạp dữ liệu mẫu bao quát toàn bộ thực thể và các ca kiểm thử nghiệp vụ.
* **Cấu hình mẫu**:
  * [`Project/src/db.properties.example`](Project/src/db.properties.example): Template mẫu cấu hình kết nối database.

---

### 1.2. Những phần mềm BẮT BUỘC PHẢI CÀI ĐẶT trên máy cá nhân
Trước khi chạy dự án, máy tính của bạn cần đảm bảo đã cài đặt các phần mềm sau:

1. **Java Development Kit (JDK 11 trở lên)**:
   * *Yêu cầu*: Tối thiểu JDK 11 (khuyến nghị OpenJDK 11, JDK 17 LTS hoặc JDK 21 LTS).
   * *Kiểm tra phiên bản trên máy*:
     ```bash
     java -version
     javac -version
     ```
   * *Cài đặt trên Ubuntu/Debian (Linux)*:
     ```bash
     sudo apt update
     sudo apt install openjdk-11-jdk
     ```
   * *Cài đặt trên Windows / macOS*: Tải bộ cài từ [Eclipse Adoptium (Temurin)](https://adoptium.net/) hoặc Oracle JDK.

2. **Hệ quản trị CSDL MySQL Server (MySQL 8.0 trở lên)**:
   * *Yêu cầu*: MySQL Server đang khởi chạy trên cổng `3306`.
   * *Kiểm tra dịch vụ MySQL*:
     ```bash
     mysql --version
     sudo systemctl status mysql
     ```
   * *Cài đặt trên Ubuntu/Debian (Linux)*:
     ```bash
     sudo apt update
     sudo apt install mysql-server
     sudo systemctl start mysql
     ```
   * *Cài đặt trên Windows*: Sử dụng bộ cài MySQL Installer (chọn MySQL Server 8.0) hoặc XAMPP/WampServer (chú ý cấu hình port 3306).

3. **Môi trường phát triển (IDE)**:
   * **Khuyến nghị**: **Eclipse IDE for Java Developers** (vì dự án đã chứa sẵn cấu hình `.project` và `.classpath` tương thích 100%).
   * *Lựa chọn thay thế*: Có thể sử dụng **IntelliJ IDEA** hoặc **VS Code** (kèm bộ extension *Extension Pack for Java*).

---

### 1.3. Các bước thiết lập & Chạy dự án từng bước (Step-by-Step)

#### Bước 1: Khởi tạo Cơ sở Dữ liệu MySQL
Mở Terminal (hoặc MySQL Workbench) tại thư mục dự án và thực thi lần lượt 2 script:
```bash
# 1. Tạo database library_db và 15 bảng
mysql -u root -p < schema.sql

# 2. Nạp dữ liệu mẫu
mysql -u root -p < sample_data.sql
```

#### Bước 2: Cấu hình thông tin kết nối Cơ sở Dữ liệu
1. Mở file [`Project/src/db.properties`](Project/src/db.properties).
2. Điền chính xác tài khoản (`db.username`) và mật khẩu (`db.password`) MySQL trên máy của bạn:
   ```properties
   db.url=jdbc:mysql://localhost:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
   db.username=root
   db.password=ĐIỀN_MẬT_KHẨU_MYSQL_CỦA_BẠN
   db.driver=com.mysql.cj.jdbc.Driver
   ```

#### Bước 3: Mở dự án trong Eclipse & Đồng bộ thư viện
1. Khởi động Eclipse $\rightarrow$ Chọn menu **File** $\rightarrow$ **Import...**
2. Chọn **General** $\rightarrow$ **Existing Projects into Workspace** $\rightarrow$ Bấm **Next**.
3. Tại ô **Select root directory**, bấm **Browse...** và trỏ đến thư mục `Project` (nằm bên trong thư mục `database_project`). Bấm **Finish**.
4. **Đồng bộ thư viện & File mới**:
   * Nhấp chuột phải vào tên dự án `Project` trong cửa sổ *Package Explorer* $\rightarrow$ Chọn **Refresh** (hoặc nhấn phím **`F5`**).
   * Kiểm tra trong mục **Referenced Libraries**: Đảm bảo file `mysql-connector-j-26.7.0.jar` đã hiển thị bình thường.
   * (Nếu có cảnh báo): Vào menu **Project** $\rightarrow$ **Clean...** $\rightarrow$ Bấm **Clean**.

#### Bước 4: Chạy kiểm thử ứng dụng
* Mở file [`Project/src/com/library/Main.java`](Project/src/com/library/Main.java).
* Nhấp chuột phải $\rightarrow$ Chọn **Run As** $\rightarrow$ **Java Application**.
* Ứng dụng sẽ tự động kiểm tra kết nối MySQL, truy vấn dữ liệu từ bảng `BORROW`, tính toán hạn trễ và tiền phạt trực tiếp từ database.

---

## 2. Tổng quan Kiến trúc Hệ thống

```
           [ LibraryController ]          <- Tầng Điều phối Sự kiện (Controller)
                    │
                    ▼
            [ BorrowService ]             <- Tầng Xử lý Nghiệp vụ & Tính toán phạt (Service)
                    │
                    ▼
           [ BorrowRepository ]           <- Tầng Truy xuất Dữ liệu JDBC (Repository)
             │              │
             ▼              ▼
    [ DatabaseConnection ] [ Models (15 POJOs) ]
             │
             ▼
     [ MySQL Database ] (schema.sql & sample_data.sql)
```

---

## 3. Chi tiết các thành phần đã được hiện thực (Coded Components)

### 3.1. Tầng Cơ sở Dữ liệu (MySQL Layer)
* [`schema.sql`](schema.sql):
  * Định nghĩa **15 bảng** chuẩn InnoDB, mã hóa UTF-8 MB4.
  * Hiện thực quan hệ kế thừa (Inheritance/Subtypes):
    * `PERSON` (Superclass) $\rightarrow$ `UNIVERSITY_MEMBER` & `EXTERNAL_AUTHOR` (Subclasses).
    * `RESOURCE` (Superclass) $\rightarrow$ `PHYSICAL_BOOK` & `DIGITAL_JOURNAL` (Subclasses).
  * Các bảng quan hệ nhiều-nhiều (N:M): `AUTHOR_OF`, `RESOURCE_LICENSE`.
  * Nghiệp vụ mượn trả & tài chính: `BOOK_COPY`, `BORROW`, `FINE`, `PAYMENT_TRANSACTION`.
  * Phân quyền bản quyền tài liệu số: `LICENSE_PACKAGE`, `GRANTED_LICENSE`.
  * **Tối ưu hiệu năng ghi (Ràng buộc BR13)**: Bảng `ACCESS_LOG` tách rời Foreign Key khỏi Master tables, dùng Independent Indexes để tối ưu tốc độ ghi log.
* [`sample_data.sql`](sample_data.sql):
  * Dữ liệu mẫu kiểm thử bao quát toàn bộ 15 bảng và 4 kịch bản nghiệp vụ (Validation Cases): Phân quyền mượn sách theo vai trò người dùng, kiểm soát hạn bản quyền số, tự động phạt trễ hạn, và thanh toán trả góp tiền phạt.

---

### 3.2. Tầng Thực thể Dữ liệu (`com.library.model`)
Gồm **15 lớp POJO** ánh xạ 1:1 với cấu trúc bảng trong MySQL, đầy đủ attributes, constructors (no-arg và parameterized), getters/setters, và phương thức `toString()`:

| Nhóm | Lớp Model (POJO) | Mô tả |
| :--- | :--- | :--- |
| **Người dùng** | [`Person.java`](Project/src/com/library/model/Person.java) | Superclass lưu trữ thông tin cá nhân dùng chung |
| | [`UniversityMember.java`](Project/src/com/library/model/UniversityMember.java) | Kế thừa `Person`, đại diện cho Sinh viên hoặc Giảng viên |
| | [`ExternalAuthor.java`](Project/src/com/library/model/ExternalAuthor.java) | Kế thừa `Person`, đại diện cho Tác giả bên ngoài |
| **Tài nguyên** | [`Resource.java`](Project/src/com/library/model/Resource.java) | Superclass danh mục tài nguyên chung |
| | [`PhysicalBook.java`](Project/src/com/library/model/PhysicalBook.java) | Kế thừa `Resource`, đại diện cho Sách in |
| | [`DigitalJournal.java`](Project/src/com/library/model/DigitalJournal.java) | Kế thừa `Resource`, đại diện cho Tạp chí / Bài báo số |
| | [`BookCopy.java`](Project/src/com/library/model/BookCopy.java) | Bản sao vật lý của sách kèm mã vạch và trạng thái |
| **Bản quyền** | [`LicensePackage.java`](Project/src/com/library/model/LicensePackage.java) | Danh mục các gói bản quyền tài liệu số |
| | [`GrantedLicense.java`](Project/src/com/library/model/GrantedLicense.java) | Quyền truy cập gói tài liệu cấp cho người dùng |
| | [`ResourceLicense.java`](Project/src/com/library/model/ResourceLicense.java) | Bảng kết hợp (N:M) giữa tài nguyên số và gói bản quyền |
| **Tác giả** | [`AuthorOf.java`](Project/src/com/library/model/AuthorOf.java) | Bảng kết hợp (N:M) tác giả và tài nguyên |
| **Mượn & Phạt** | [`Borrow.java`](Project/src/com/library/model/Borrow.java) | Thông tin giao dịch mượn trả sách |
| | [`Fine.java`](Project/src/com/library/model/Fine.java) | Phiếu phạt phát sinh khi vi phạm quy chế |
| | [`PaymentTransaction.java`](Project/src/com/library/model/PaymentTransaction.java) | Lịch sử các đợt thanh toán tiền phạt |
| **Nhật ký** | [`AccessLog.java`](Project/src/com/library/model/AccessLog.java) | Lịch sử truy cập và tải tài liệu số |

---

### 3.3. Tầng Tiện ích & Kết nối Cơ sở Dữ liệu (`com.library.util`)
* [`DatabaseConnection.java`](Project/src/com/library/util/DatabaseConnection.java):
  * Nạp cấu hình tự động từ file [`db.properties`](Project/src/db.properties).
  * Đăng ký JDBC Driver `com.mysql.cj.jdbc.Driver`.
  * Cung cấp phương thức `getConnection()` để cấp phát kết nối an toàn.
  * Cung cấp phương thức `testConnection()` kiểm tra trạng thái máy chủ MySQL.
* [`db.properties.example`](Project/src/db.properties.example): File cấu hình mẫu để các thành viên thiết lập tài khoản MySQL cá nhân.

---

### 3.4. Tầng Truy xuất Dữ liệu (`com.library.repository`)
* [`BorrowRepository.java`](Project/src/com/library/repository/BorrowRepository.java):
  * Quản lý tương tác trực tiếp với MySQL qua `PreparedStatement` và cơ chế tự động đóng tài nguyên `try-with-resources`.
  * `insertBorrow(Borrow borrow)`: Thêm mới phiếu mượn và tự động gán `borrowId` sinh từ database.
  * `findById(int borrowId)`: Tìm phiếu mượn theo mã ID.
  * `findOverdueBorrows()`: Truy vấn danh sách mượn sách đang trễ hạn (`return_date IS NULL AND due_date < CURRENT_DATE()`).
  * `findByMemberId(int memberPersonId)`: Lịch sử mượn sách của một thành viên.
  * `updateBorrow(Borrow borrow)`: Cập nhật thông tin phiếu mượn.
  * `deleteBorrow(int borrowId)`: Xóa phiếu mượn.
  * `findAll()`: Lấy danh sách toàn bộ phiếu mượn.
  * `isCopyAvailable(int copyId)` & `updateCopyAvailability(int copyId, String status)`: Kiểm tra và cập nhật trạng thái bản sao sách trong `BOOK_COPY`.

---

### 3.5. Tầng Nghiệp vụ (`com.library.service`)
* [`BorrowService.java`](Project/src/com/library/service/BorrowService.java):
  * `processNewBorrow(int memberId, int copyId)`: Kiểm tra tình trạng bản sao sách, thiết lập ngày mượn hiện tại và hạn trả (mặc định 14 ngày), lưu dữ liệu và chuyển trạng thái bản sao sang `Borrowed`.
  * `returnBook(int borrowId)`: Ghi nhận ngày trả sách thực tế, mở lại bản sao sang `Available`.
  * `calculateLatePenalty(int borrowId)`: Tự động tính tiền phạt dựa trên số ngày trả trễ so với `due_date` (10,000 VND / ngày).
  * `getOverdueBorrows()`: Danh sách các giao dịch đang bị quá hạn.
  * `getBorrowDetails(int borrowId)`: Xem chi tiết giao dịch.

---

### 3.6. Tầng Điều phối Sự kiện (`com.library.controller`)
* [`LibraryController.java`](Project/src/com/library/controller/LibraryController.java):
  * `handleBorrowRequest(int memberId, int copyId)`: Xử lý luồng yêu cầu mượn sách từ người dùng và đưa ra thông báo kết quả.
  * `handleReturnRequest(int borrowId)`: Xử lý yêu cầu trả sách và thông báo tiền phạt nếu có.
  * `displayOverdueMembers()`: In danh sách tổng hợp các lượt mượn quá hạn kèm tiền phạt ước tính.
  * `displayBorrowHistory(int borrowId)`: In chi tiết thông tin mượn trả.

---

### 3.7. Chương trình Chạy Kiểm thử (`com.library.Main`)
* [`Main.java`](Project/src/com/library/Main.java):
  * Điểm chạy chính (Entry Point) của ứng dụng.
  * Tự động kiểm tra kết nối tới MySQL.
  * Gọi Controller, Service và Repository để kiểm tra luồng đọc dữ liệu từ MySQL và hiển thị kết quả trực tiếp trên console.

---

## 4. Cấu trúc Thư mục Dự án

```
database_project/
├── .gitignore                      # Cấu hình bỏ qua file rác khi đẩy code lên Git
├── README.md                       # Tài liệu mô tả dự án và hướng dẫn sử dụng
├── schema.sql                      # Script tạo cơ sở dữ liệu và 15 bảng MySQL
├── sample_data.sql                 # Script nạp dữ liệu mẫu phục vụ kiểm thử
├── erDiagram.md                    # Bản vẽ cấu trúc thực thể Mermaid ERD
├── G5_phase1.pdf                   # Báo cáo đặc tả yêu cầu hệ thống (Phase 1)
└── Project/                        # Thư mục dự án Java (Eclipse Project)
    ├── .project                    # Cấu hình project của Eclipse
    ├── .classpath                  # Cấu hình classpath và thư viện của Eclipse
    ├── .gitignore                  # Bỏ qua thư mục bin/ của Project
    ├── lib/
    │   └── mysql-connector-j-26.7.0.jar  # Driver kết nối MySQL JDBC
    └── src/
        ├── db.properties           # File cấu hình kết nối MySQL cục bộ
        ├── db.properties.example   # Template mẫu file cấu hình cho thành viên mới
        └── com/
            └── library/
                ├── Main.java       # File chạy demo kiểm thử toàn bộ luồng
                ├── controller/
                │   └── LibraryController.java
                ├── service/
                │   └── BorrowService.java
                ├── repository/
                │   └── BorrowRepository.java
                ├── util/
                │   └── DatabaseConnection.java
                └── model/          # 15 Lớp thực thể POJO
                    ├── Person.java
                    ├── UniversityMember.java
                    ├── ExternalAuthor.java
                    ├── Resource.java
                    ├── PhysicalBook.java
                    ├── DigitalJournal.java
                    ├── BookCopy.java
                    ├── LicensePackage.java
                    ├── GrantedLicense.java
                    ├── ResourceLicense.java
                    ├── AuthorOf.java
                    ├── Borrow.java
                    ├── Fine.java
                    ├── PaymentTransaction.java
                    └── AccessLog.java
```

---

## 5. Quy định Cộng tác Git (.gitignore)
* Hệ thống đã cấu hình đầy đủ file [`.gitignore`](.gitignore) ở thư mục gốc và thư mục dự án con.
* **Những thành phần KHÔNG ĐƯỢC push lên Git**:
  * Thư mục cấu hình workspace của Eclipse: `/.metadata/`, `.recommenders/`.
  * Các file biên dịch nhị phân: `bin/`, `*.class`, `*.log`.
  * Thư mục cấu hình IDE cá nhân: `.idea/`, `.vscode/`, `*.iml`.
  * Các file rác hệ điều hành: `.DS_Store`, `Thumbs.db`.

